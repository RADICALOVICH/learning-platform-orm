package ru.mifi.learning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.mifi.learning.dto.assignment.AssignmentRequest;
import ru.mifi.learning.dto.assignment.GradeRequest;
import ru.mifi.learning.dto.assignment.SubmitRequest;
import ru.mifi.learning.exception.ConflictException;
import ru.mifi.learning.support.AbstractIntegrationTest;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
class AssignmentServiceIT extends AbstractIntegrationTest {
    private final AssignmentService assignments;
    private final SubmissionService solutions;

    @Test
    @DisplayName("Максимум задания можно снизить до выставленной оценки, но не ниже неё")
    void preserveAwardedGrade() {
        var course = data.publishedCourseWithLessons(1);
        long lesson = course.getModules().getFirst().getLessons().getFirst().getId();
        long student = data.enrollment(course).getStudent().getId();
        var assignment = assignments.create(lesson, new AssignmentRequest("ДЗ", "Описание", null, 10));
        var submission = solutions.submit(assignment.id(), new SubmitRequest(student, "Ответ"));
        solutions.grade(submission.id(), new GradeRequest(8, null));
        assertThatThrownBy(
                        () -> assignments.update(assignment.id(), new AssignmentRequest("Другое", "Описание", null, 7)))
                .extracting("code")
                .isEqualTo("ASSIGNMENT_MAX_BELOW_GRADE");
        assertThat(assignments.get(assignment.id())).isEqualTo(assignment);
        var updated = assignments.update(assignment.id(), new AssignmentRequest("ДЗ", "Описание", null, 8));
        assertThat(updated.maxScore()).isEqualTo(8);
        assertThatThrownBy(() -> solutions.grade(submission.id(), new GradeRequest(9, null)))
                .extracting("code")
                .isEqualTo("SCORE_OUT_OF_RANGE");
        assertThat(solutions.get(submission.id()).score()).isEqualTo(8);
    }

    @Test
    @DisplayName("Проекция читает задания студента одним запросом и показывает статус сдачи")
    void projection() {
        var c = data.publishedCourseWithLessons(1);
        var module = c.getModules().getFirst();
        var lesson = module.getLessons().getFirst();
        var e = data.enrollment(c);
        long student = e.getStudent().getId();
        var a = assignments.create(lesson.getId(), new AssignmentRequest("ДЗ", "Условие", null, null));
        assertThat(a.maxScore()).isEqualTo(100);
        queries.reset();
        assertThat(assignments.studentAssignments(student).getFirst().submissionId())
                .isNull();
        queries.assertCount(1);
        var s = solutions.submit(a.id(), new SubmitRequest(student, "Ответ"));
        assertThat(assignments.studentAssignments(student).getFirst().submissionId())
                .isEqualTo(s.id());
        assertThatThrownBy(() -> assignments.delete(a.id())).isInstanceOf(ConflictException.class);
        solutions.delete(s.id());
        assignments.delete(a.id());
    }
}
