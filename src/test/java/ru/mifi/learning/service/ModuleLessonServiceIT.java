package ru.mifi.learning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

class ModuleLessonServiceIT extends AbstractIntegrationTest {
    @Autowired
    ModuleService modules;

    @Autowired
    LessonService lessons;

    @Autowired
    ProgressService progress;

    @Autowired
    QuizRepository quizzes;

    @Autowired
    LessonProgressRepository marks;

    @Autowired
    SubmissionRepository submissions;

    @Autowired
    AssignmentService assignments;

    @Autowired
    SubmissionService solutions;

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
        modules.delete(module.getId());
        assertThat(quizzes.existsById(q.getId())).isFalse();
        assertThat(marks.countByEnrollmentId(e.getId())).isZero();
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
