package ru.mifi.learning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.mifi.learning.domain.enums.EnrollmentStatus;
import ru.mifi.learning.dto.assignment.AssignmentRequest;
import ru.mifi.learning.dto.assignment.SubmitRequest;
import ru.mifi.learning.dto.course.LessonRequest;
import ru.mifi.learning.dto.course.ModuleRequest;
import ru.mifi.learning.dto.enrollment.StudentRequest;
import ru.mifi.learning.exception.ConflictException;
import ru.mifi.learning.repository.LessonProgressRepository;
import ru.mifi.learning.repository.QuizRepository;
import ru.mifi.learning.repository.SubmissionRepository;
import ru.mifi.learning.support.AbstractIntegrationTest;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
class ModuleLessonServiceIT extends AbstractIntegrationTest {
    private final ModuleService modules;
    private final LessonService lessons;
    private final ProgressService progress;
    private final QuizRepository quizzes;
    private final LessonProgressRepository marks;
    private final SubmissionRepository submissions;
    private final AssignmentService assignments;
    private final SubmissionService solutions;

    @Test
    @DisplayName("После максимального индекса автоматическое добавление модуля или урока возвращает конфликт")
    void orderLimit() {
        var course = data.publishedCourseWithLessons(1);
        var last = modules.create(course.getId(), new ModuleRequest("Последний", null, Integer.MAX_VALUE));
        assertThatThrownBy(() -> modules.create(course.getId(), new ModuleRequest("Следующий", null, null)))
                .extracting("code")
                .isEqualTo("ORDER_INDEX_EXHAUSTED");
        assertThat(modules.list(course.getId())).hasSize(2);
        lessons.create(last.id(), new LessonRequest("Последний", null, null, null, Integer.MAX_VALUE));
        assertThatThrownBy(() -> lessons.create(last.id(), new LessonRequest("Следующий", null, null, null, null)))
                .extracting("code")
                .isEqualTo("ORDER_INDEX_EXHAUSTED");
        assertThat(lessons.list(last.id())).hasSize(1);
    }

    @Test
    @DisplayName("Порядок новых модулей и уроков следует за максимальным индексом")
    void appendEdit() {
        var c = data.publishedCourseWithLessons(1);
        var module = c.getModules().getFirst();
        var lesson = module.getLessons().getFirst();
        modules.create(c.getId(), new ModuleRequest("Раздел", null, 9));
        var m = modules.create(c.getId(), new ModuleRequest("Следующий", null, null));
        assertThat(m.orderIndex()).isEqualTo(10);
        var l = lessons.create(module.getId(), new LessonRequest("Ещё", null, null, null, null));
        assertThat(l.orderIndex()).isOne();
        assertThat(lessons.update(l.id(), new LessonRequest("Новый", "Текст", null, 20, 4))
                        .content())
                .isEqualTo("Текст");
        assertThat(modules.update(m.id(), new ModuleRequest("Обновлён", null, 3))
                        .title())
                .isEqualTo("Обновлён");
    }

    @Test
    @DisplayName("Удаление модуля каскадно очищает тест и отметку")
    void deleteModule() {
        var c = data.publishedCourseWithLessons(1);
        var module = c.getModules().getFirst();
        var lesson = module.getLessons().getFirst();
        var e = data.enrollment(c);
        long student = e.getStudent().getId();
        var q = data.quizWithQuestions(module, 1);
        progress.mark(lesson.getId(), new StudentRequest(student));
        var retained = modules.create(c.getId(), new ModuleRequest("Сохраняемый раздел", null, null));
        var remainingLesson = lessons.create(retained.id(), new LessonRequest("Практика", null, null, null, null));
        progress.mark(remainingLesson.id(), new StudentRequest(student));
        modules.delete(module.getId());
        var result = progress.get(c.getId(), student);
        assertThat(result.completedLessons()).isOne();
        assertThat(result.totalQuizzes()).isZero();
        assertThat(result.status()).isEqualTo(EnrollmentStatus.COMPLETED);
        assertThat(quizzes.existsById(q.getId())).isFalse();
        assertThat(marks.countByEnrollmentId(e.getId())).isOne();
    }

    @Test
    @DisplayName("Последний урок опубликованного курса защищён и при удалении всего модуля")
    void keepLastPublishedLesson() {
        var course = data.publishedCourseWithLessons(1);
        var module = course.getModules().getFirst();
        var lesson = module.getLessons().getFirst();
        assertThatThrownBy(() -> lessons.delete(lesson.getId()))
                .extracting("code")
                .isEqualTo("COURSE_REQUIRES_LESSON");
        assertThatThrownBy(() -> modules.delete(module.getId()))
                .extracting("code")
                .isEqualTo("COURSE_REQUIRES_LESSON");
        assertThat(lessons.get(lesson.getId()).moduleId()).isEqualTo(module.getId());
    }

    @Test
    @DisplayName("Последний урок архива нужен активному студенту, после отписки урок можно удалить")
    void archivedLastLesson(@Autowired CourseService courses, @Autowired EnrollmentService enrollments) {
        var course = data.publishedCourseWithLessons(1);
        var enrollment = data.enrollment(course);
        var lesson = course.getModules().getFirst().getLessons().getFirst();
        courses.archive(course.getId());
        assertThatThrownBy(() -> lessons.delete(lesson.getId()))
                .extracting("code")
                .isEqualTo("COURSE_REQUIRES_LESSON");
        enrollments.unsubscribe(course.getId(), enrollment.getStudent().getId());
        lessons.delete(lesson.getId());
        assertThat(lessons.list(lesson.getModule().getId())).isEmpty();
    }

    @Test
    @DisplayName("Два параллельных удаления оставляют один урок опубликованного курса")
    void concurrentDeletion() throws Exception {
        var course = data.publishedCourseWithLessons(2);
        var module = course.getModules().getFirst();
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var futures = module.getLessons().stream()
                    .map(lesson -> pool.submit((Callable<String>) () -> {
                        start.await();
                        try {
                            lessons.delete(lesson.getId());
                            return "deleted";
                        } catch (ConflictException ex) {
                            return ex.getCode();
                        }
                    }))
                    .toList();
            start.countDown();
            assertThat(List.of(
                            futures.getFirst().get(10, TimeUnit.SECONDS),
                            futures.getLast().get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("deleted", "COURSE_REQUIRES_LESSON");
        }
        assertThat(lessons.list(module.getId())).hasSize(1);
    }

    @Test
    @DisplayName("Решение студента защищает урок и модуль от удаления")
    void protectLesson() {
        var c = data.publishedCourseWithLessons(1);
        var module = c.getModules().getFirst();
        var lesson = module.getLessons().getFirst();
        var e = data.enrollment(c);
        long student = e.getStudent().getId();
        var a = assignments.create(lesson.getId(), new AssignmentRequest("Практика", "Описание", null, 10));
        solutions.submit(a.id(), new SubmitRequest(student, "Ответ"));
        assertThatThrownBy(() -> lessons.delete(lesson.getId())).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> modules.delete(module.getId())).isInstanceOf(ConflictException.class);
    }
}
