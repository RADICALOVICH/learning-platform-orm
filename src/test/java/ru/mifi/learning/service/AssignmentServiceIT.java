package ru.mifi.learning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.mifi.learning.dto.assignment.AssignmentRequest;
import ru.mifi.learning.dto.assignment.SubmitRequest;
import ru.mifi.learning.exception.ConflictException;
import ru.mifi.learning.support.AbstractIntegrationTest;

class AssignmentServiceIT extends AbstractIntegrationTest {
    @Autowired
    AssignmentService assignments;

    @Autowired
    SubmissionService solutions;

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
