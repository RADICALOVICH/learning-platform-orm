package ru.mifi.learning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import ru.mifi.learning.domain.enums.EnrollmentStatus;
import ru.mifi.learning.dto.course.LessonRequest;
import ru.mifi.learning.dto.enrollment.StudentRequest;
import ru.mifi.learning.dto.quiz.QuizAnswerRequest;
import ru.mifi.learning.dto.quiz.TakeQuizRequest;
import ru.mifi.learning.exception.BusinessRuleException;
import ru.mifi.learning.service.event.CourseCompletedEvent;
import ru.mifi.learning.support.AbstractIntegrationTest;

@RecordApplicationEvents
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class ProgressServiceIT extends AbstractIntegrationTest {
    private final ProgressService progress;
    private final CourseService courses;
    private final LessonService lessons;
    private final QuizSubmissionService attempts;

    @Test
    @DisplayName("Отметка идемпотентна; завершивший студент остаётся в статусе COMPLETED")
    void mark(@Autowired ApplicationEvents events) {
        var c = data.publishedCourseWithLessons(1);
        var module = c.getModules().getFirst();
        var lesson = module.getLessons().getFirst();
        var e = data.enrollment(c);
        long student = e.getStudent().getId();
        courses.archive(c.getId());
        assertThat(progress.mark(lesson.getId(), new StudentRequest(student)).created())
                .isTrue();
        assertThat(progress.mark(lesson.getId(), new StudentRequest(student)).created())
                .isFalse();
        var snapshot = progress.get(c.getId(), student);
        assertThat(snapshot.status()).isEqualTo(EnrollmentStatus.COMPLETED);
        assertThat(snapshot.percent()).isEqualTo(100);
        lessons.create(module.getId(), new LessonRequest("Ещё", null, null, null, null));
        assertThat(progress.get(c.getId(), student).status()).isEqualTo(EnrollmentStatus.COMPLETED);
        assertThat(events.stream(CourseCompletedEvent.class)).hasSize(1);
    }

    @Test
    @DisplayName("Урок другого курса нельзя отметить")
    void foreignLesson() {
        var c = data.publishedCourseWithLessons(1);
        var module = c.getModules().getFirst();
        var lesson = module.getLessons().getFirst();
        var e = data.enrollment(c);
        long student = e.getStudent().getId();
        var other = data.publishedCourseWithLessons(1)
                .getModules()
                .getFirst()
                .getLessons()
                .getFirst();
        assertThatThrownBy(() -> progress.mark(other.getId(), new StudentRequest(student)))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code")
                .isEqualTo("LESSON_NOT_IN_COURSE");
    }

    @Test
    @DisplayName("Успешная попытка после последнего урока завершает курс")
    void lastQuiz(@Autowired ApplicationEvents events) {
        var c = data.publishedCourseWithLessons(1);
        var module = c.getModules().getFirst();
        var lesson = module.getLessons().getFirst();
        var e = data.enrollment(c);
        long student = e.getStudent().getId();
        var quiz = data.quizWithQuestions(module, 1);
        var q = quiz.getQuestions().getFirst();
        progress.mark(lesson.getId(), new StudentRequest(student));
        assertThat(progress.get(c.getId(), student).status()).isEqualTo(EnrollmentStatus.ACTIVE);
        attempts.submit(quiz.getId(), new TakeQuizRequest(student, List.of()));
        assertThat(events.stream(CourseCompletedEvent.class)).isEmpty();
        attempts.submit(
                quiz.getId(),
                new TakeQuizRequest(
                        student, List.of(new QuizAnswerRequest(q.getId(), List.copyOf(q.correctOptionIds())))));
        assertThat(progress.get(c.getId(), student).status()).isEqualTo(EnrollmentStatus.COMPLETED);
        assertThat(events.stream(CourseCompletedEvent.class)).hasSize(1);
    }
}
