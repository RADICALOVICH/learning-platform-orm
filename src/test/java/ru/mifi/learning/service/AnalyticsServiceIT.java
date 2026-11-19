package ru.mifi.learning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import jakarta.persistence.EntityManagerFactory;
import java.util.Arrays;
import java.util.List;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.mifi.learning.domain.entity.Course;
import ru.mifi.learning.domain.entity.Quiz;
import ru.mifi.learning.dto.analytics.LeaderboardEntry;
import ru.mifi.learning.dto.analytics.RatedCourse;
import ru.mifi.learning.dto.assignment.AssignmentRequest;
import ru.mifi.learning.dto.assignment.GradeRequest;
import ru.mifi.learning.dto.assignment.SubmitRequest;
import ru.mifi.learning.dto.course.ModuleRequest;
import ru.mifi.learning.dto.enrollment.StudentRequest;
import ru.mifi.learning.dto.quiz.QuizAnswerRequest;
import ru.mifi.learning.dto.quiz.TakeQuizRequest;
import ru.mifi.learning.dto.review.CreateReviewRequest;
import ru.mifi.learning.exception.NotFoundException;
import ru.mifi.learning.repository.CourseModuleRepository;
import ru.mifi.learning.repository.CourseRepository;
import ru.mifi.learning.support.AbstractIntegrationTest;

class AnalyticsServiceIT extends AbstractIntegrationTest {
    @Autowired
    AnalyticsService analytics;

    @Autowired
    AssignmentService assignments;

    @Autowired
    SubmissionService submissions;

    @Autowired
    QuizSubmissionService attempts;

    @Autowired
    ProgressService progress;

    @Autowired
    ReviewService reviews;

    @Autowired
    EnrollmentService enrollments;

    @Autowired
    ModuleService modules;

    @Autowired
    CourseModuleRepository moduleRepository;

    @Autowired
    CourseRepository courses;

    @Autowired
    EntityManagerFactory emf;

    @Test
    @DisplayName("Успеваемость разделяется по курсам и учитывает только оценённые задания и разные сданные тесты")
    void student() {
        var course = data.publishedCourseWithLessons(2);
        var extra = data.publishedCourseWithLessons(1);
        long student = member(course);
        enrollments.enroll(extra.getId(), new StudentRequest(student));
        var first = task(course, 8);
        var second = task(course, 4);
        grade(first, student, 5);
        grade(second, student, 3);
        grade(task(extra, 100), student, 0);
        submissions.submit(task(course, 10), new SubmitRequest(student, "Ожидает проверки"));
        progress.mark(course.getModules().getFirst().getLessons().getFirst().getId(), new StudentRequest(student));
        var quiz = quiz(course, 2);
        attempt(quiz, student, 0, 1);
        attempt(quiz, student, 0, 1);
        attempt(quiz, student);
        queries.reset();
        var result = analytics.student(student);
        assertThat(result).hasSize(2);
        var row = result.getFirst();
        assertThat(row.courseId()).isEqualTo(course.getId());
        assertThat(row.lessonCompletionPercent()).isEqualTo(50);
        assertThat(row.averageAssignmentPercent()).isEqualTo(68.75);
        assertThat(row.bestQuizPercent()).isEqualTo(100);
        assertThat(row.passedQuizzes()).isOne();
        var other = result.getLast();
        assertThat(other.courseId()).isEqualTo(extra.getId());
        assertThat(other.averageAssignmentPercent()).isZero();
        assertThat(other.lessonCompletionPercent()).isZero();
        assertThat(other.bestQuizPercent()).isNull();
        assertProjectionQueries(1);
    }

    @Test
    @DisplayName("Сводка курса сохраняет знаменатели при нескольких записях, отзывах и попытках")
    void course() {
        var c = data.publishedCourseWithLessons(1);
        long alice = member(c), bob = member(c), third = member(c);
        var q = quiz(c, 2);
        attempt(q, alice, 0, 1);
        attempt(q, bob, 0);
        attempt(q, bob);
        progress.mark(c.getModules().getFirst().getLessons().getFirst().getId(), new StudentRequest(alice));
        review(c, alice, 5);
        review(c, bob, 3);
        long assignment = task(c, 8);
        grade(assignment, alice, 5);
        grade(assignment, bob, 8);
        submissions.submit(assignment, new SubmitRequest(third, "Без оценки"));
        queries.reset();
        var r = analytics.course(c.getId());
        assertThat(r.enrolledStudents()).isEqualTo(3);
        assertThat(r.completedStudents()).isOne();
        assertThat(r.completionPercent()).isCloseTo(100d / 3, within(0.000001));
        assertThat(r.averageRating()).isEqualTo(4);
        assertThat(r.reviewCount()).isEqualTo(2);
        assertThat(r.averageAssignmentPercent()).isEqualTo(81.25);
        assertThat(r.quizPassPercent()).isCloseTo(100d / 3, within(0.000001));
        assertProjectionQueries(1);
    }

    @Test
    @DisplayName("Лидерборд учитывает лучшую попытку каждого теста, изолирует курс и присваивает равные места")
    void leaderboard() {
        var c = data.publishedCourseWithLessons(1);
        long alice = member(c), bob = member(c), third = member(c);
        var q = quiz(c, 2);
        var module = modules.create(c.getId(), new ModuleRequest("Практика", null, null));
        var otherQuiz = data.quizWithQuestions(moduleRepository.getReferenceById(module.id()), 1);
        attempt(q, alice, 0, 1);
        attempt(q, alice, 0);
        attempt(otherQuiz, alice, 0);
        long assignment = task(c, 10);
        grade(assignment, alice, 5);
        grade(assignment, bob, 8);
        var outside = data.publishedCourseWithLessons(1);
        enrollments.enroll(outside.getId(), new StudentRequest(third));
        grade(task(outside, 100), third, 100);
        queries.reset();
        var result = analytics.leaderboard(c.getId(), 10);
        assertThat(result).extracting(LeaderboardEntry::studentId).containsExactly(alice, bob, third);
        assertThat(result).extracting(LeaderboardEntry::totalPoints).containsExactly(8L, 8L, 0L);
        assertThat(result).extracting(LeaderboardEntry::rank).containsExactly(1L, 1L, 3L);
        assertProjectionQueries(1);
        assertThat(analytics.leaderboard(c.getId(), 1)).containsExactly(result.getFirst());
    }

    @Test
    @DisplayName("Статистика теста учитывает пропуски, повторные попытки и долю верных ответов каждого вопроса")
    void quizResults() {
        var c = data.publishedCourseWithLessons(1);
        var q = quiz(c, 2);
        long alice = member(c), bob = member(c);
        attempt(q, alice, 0, 1);
        attempt(q, alice, 0);
        attempt(q, bob);
        queries.reset();
        var r = analytics.quiz(q.getId());
        assertThat(r.statistics().attempts()).isEqualTo(3);
        assertThat(r.statistics().uniqueStudents()).isEqualTo(2);
        assertThat(r.statistics().averageScorePercent()).isEqualTo(50);
        assertThat(r.statistics().passPercent()).isCloseTo(100d / 3, within(0.000001));
        assertThat(r.questions()).hasSize(2);
        assertThat(r.questions().getFirst().answers()).isEqualTo(3);
        assertThat(r.questions().getFirst().correctPercent()).isCloseTo(200d / 3, within(0.000001));
        assertThat(r.questions().getLast().correctPercent()).isCloseTo(100d / 3, within(0.000001));
        assertThat(r.hardestQuestionId()).isEqualTo(q.getQuestions().getLast().getId());
        assertProjectionQueries(1);
    }

    @Test
    @DisplayName("Топ курсов применяет порог отзывов перед лимитом и разрешает равенство рейтингов")
    void topRated() {
        var first = data.publishedCourseWithLessons(1);
        var second = data.publishedCourseWithLessons(1);
        var third = data.publishedCourseWithLessons(1);
        data.publishedCourseWithLessons(1);
        review(first, member(first), 4);
        review(second, member(second), 5);
        review(second, member(second), 3);
        review(third, member(third), 5);
        queries.reset();
        assertThat(analytics.topRated(0, 3))
                .extracting(RatedCourse::courseId)
                .containsExactly(third.getId(), second.getId(), first.getId());
        assertProjectionQueries(1);
        assertThat(analytics.topRated(2, 1)).containsExactly(new RatedCourse(second.getId(), second.getTitle(), 4, 2));
        assertThat(analytics.topRated(3, 5)).isEmpty();
    }

    @Test
    @DisplayName("Пустые выборки отличают отсутствующие наблюдения от нуля и от неизвестного ресурса")
    void empty() {
        var c = courses.save(data.course());
        var r = analytics.course(c.getId());
        assertThat(r.enrolledStudents()).isZero();
        assertThat(r.completedStudents()).isZero();
        assertThat(r.reviewCount()).isZero();
        assertThat(r.averageRating()).isNull();
        assertThat(r.averageAssignmentPercent()).isNull();
        assertThat(r.completionPercent()).isNull();
        assertThat(r.quizPassPercent()).isNull();
        assertThat(analytics.student(data.student().getId())).isEmpty();
        assertThat(analytics.leaderboard(c.getId(), 5)).isEmpty();
        var quiz = quiz(data.publishedCourseWithLessons(1), 1);
        var stats = analytics.quiz(quiz.getId());
        assertThat(stats.statistics().attempts()).isZero();
        assertThat(stats.statistics().passPercent()).isNull();
        assertThat(stats.statistics().averageScorePercent()).isNull();
        assertThat(stats.questions().getFirst().answers()).isZero();
        assertThat(stats.questions().getFirst().correctPercent()).isNull();
        assertThat(stats.hardestQuestionId()).isNull();
        var noQuestions =
                analytics.quiz(quiz(data.publishedCourseWithLessons(1), 0).getId());
        assertThat(noQuestions.questions()).isEmpty();
        assertThat(noQuestions.hardestQuestionId()).isNull();
        assertThatThrownBy(() -> analytics.student(Long.MAX_VALUE)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> analytics.course(Long.MAX_VALUE)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> analytics.leaderboard(Long.MAX_VALUE, 5)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> analytics.quiz(Long.MAX_VALUE)).isInstanceOf(NotFoundException.class);
    }

    private void assertProjectionQueries(long expected) {
        queries.assertCount(expected);
        assertThat(emf.unwrap(SessionFactory.class).getStatistics().getEntityLoadCount())
                .isZero();
    }

    private long member(Course c) {
        return data.enrollment(c).getStudent().getId();
    }

    private Quiz quiz(Course c, int questions) {
        return data.quizWithQuestions(c.getModules().getFirst(), questions);
    }

    private void review(Course c, long student, int rating) {
        reviews.create(c.getId(), new CreateReviewRequest(student, rating, null));
    }

    private long task(Course c, int max) {
        long lesson = c.getModules().getFirst().getLessons().getFirst().getId();
        return assignments
                .create(lesson, new AssignmentRequest("Практика", "Решите задачу", null, max))
                .id();
    }

    private void grade(long assignment, long student, int score) {
        var solution = submissions.submit(assignment, new SubmitRequest(student, "Решение"));
        submissions.grade(solution.id(), new GradeRequest(score, null));
    }

    private void attempt(Quiz quiz, long student, int... correctAnswers) {
        var answers = Arrays.stream(correctAnswers)
                .mapToObj(i -> quiz.getQuestions().get(i))
                .map(q -> new QuizAnswerRequest(q.getId(), List.copyOf(q.correctOptionIds())))
                .toList();
        attempts.submit(quiz.getId(), new TakeQuizRequest(student, answers));
    }
}
