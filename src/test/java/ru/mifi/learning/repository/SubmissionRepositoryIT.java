package ru.mifi.learning.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import ru.mifi.learning.domain.entity.Assignment;
import ru.mifi.learning.domain.entity.Course;
import ru.mifi.learning.domain.entity.Lesson;
import ru.mifi.learning.domain.entity.Submission;
import ru.mifi.learning.domain.entity.User;
import ru.mifi.learning.domain.enums.SubmissionStatus;
import ru.mifi.learning.repository.projection.StudentAssignment;
import ru.mifi.learning.support.AbstractRepositoryTest;
import ru.mifi.learning.support.TestDataFactory;

class SubmissionRepositoryIT extends AbstractRepositoryTest {
    @Autowired
    SubmissionRepository submissions;

    @Autowired
    AssignmentRepository assignments;

    @Test
    @DisplayName("Одно задание допускает одну сдачу от студента")
    void duplicate() {
        Course course = data.publishedCourseWithLessons(1);
        Assignment assignment = new Assignment("A", "Задание");
        Lesson lesson = course.getModules().getFirst().getLessons().getFirst();
        lesson.addAssignment(assignment);
        User student = data.enrollment(course).getStudent();
        em.flush();

        submissions.saveAndFlush(new Submission(assignment, student, "Текст", TestDataFactory.NOW));
        assertThatThrownBy(
                        () -> submissions.saveAndFlush(new Submission(assignment, student, "Ещё", TestDataFactory.NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Список решений загружает студента одним запросом")
    void fetchStudent() {
        Course course = data.publishedCourseWithLessons(1);
        Assignment assignment = new Assignment("A", "Задание");
        Lesson lesson = course.getModules().getFirst().getLessons().getFirst();
        lesson.addAssignment(assignment);
        User student = data.enrollment(course).getStudent();
        em.flush();

        submissions.save(new Submission(assignment, student, "Текст", TestDataFactory.NOW));
        flushAndClear();
        queries.reset();
        var result = submissions.findByAssignmentIdWithStudent(assignment.getId());
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getStudent().getFullName()).isEqualTo(student.getFullName());
        queries.assertCount(1);
    }

    @Test
    @DisplayName("Проекция возвращает сданные и открытые задания только по курсам студента")
    void projection() {
        Course course = data.publishedCourseWithLessons(1);
        Assignment assignment = new Assignment("A", "Задание");
        Lesson lesson = course.getModules().getFirst().getLessons().getFirst();
        lesson.addAssignment(assignment);
        User student = data.enrollment(course).getStudent();
        em.flush();

        Assignment open = new Assignment("Б", "Условие");
        lesson.addAssignment(open);
        Course another = data.publishedCourseWithLessons(1);
        another.getModules().getFirst().getLessons().getFirst().addAssignment(new Assignment("Чужое", "Условие"));
        em.flush();
        Submission own = submissions.save(new Submission(assignment, student, "Текст", TestDataFactory.NOW));
        own.grade(89, "Хорошо", TestDataFactory.NOW);
        submissions.save(
                new Submission(open, data.enrollment(course).getStudent(), "Чужая сдача", TestDataFactory.NOW));
        flushAndClear();
        queries.reset();
        var rows = assignments.findStudentAssignments(student.getId());
        queries.assertCount(1);
        assertThat(rows)
                .containsExactly(
                        new StudentAssignment(
                                assignment.getId(),
                                "A",
                                course.getId(),
                                course.getTitle(),
                                null,
                                own.getId(),
                                SubmissionStatus.GRADED,
                                89),
                        new StudentAssignment(
                                open.getId(), "Б", course.getId(), course.getTitle(), null, null, null, null));
    }
}
