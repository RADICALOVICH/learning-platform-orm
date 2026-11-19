package ru.mifi.learning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import ru.mifi.learning.domain.enums.NotificationType;
import ru.mifi.learning.dto.assignment.AssignmentRequest;
import ru.mifi.learning.dto.assignment.GradeRequest;
import ru.mifi.learning.dto.assignment.SubmitRequest;
import ru.mifi.learning.dto.notification.NotificationResponse;
import ru.mifi.learning.exception.BusinessRuleException;
import ru.mifi.learning.exception.ConflictException;
import ru.mifi.learning.service.event.AssignmentCreatedEvent;
import ru.mifi.learning.service.event.SubmissionGradedEvent;
import ru.mifi.learning.support.AbstractIntegrationTest;
import ru.mifi.learning.support.MutableClock;

@RecordApplicationEvents
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class SubmissionServiceIT extends AbstractIntegrationTest {
    private final SubmissionService solutions;
    private final CourseService courses;
    private final AssignmentService assignments;

    @TestBean(name = "clock", methodName = "testClock")
    Clock clock;

    static MutableClock testClock() {
        return new MutableClock(Instant.EPOCH);
    }

    @Test
    @DisplayName("Повтор оценки сохраняет время и отправляет уведомление только при реальном изменении")
    void retryGrade(@Autowired NotificationService notifications) {
        var course = data.publishedCourseWithLessons(1);
        var lesson = course.getModules().getFirst().getLessons().getFirst();
        long student = data.enrollment(course).getStudent().getId();
        var assignment = assignments.create(lesson.getId(), new AssignmentRequest("ДЗ", "Условие", null, 10));
        var submission = solutions.submit(assignment.id(), new SubmitRequest(student, "Ответ"));
        var grade = new GradeRequest(10, null);
        var first = solutions.grade(submission.id(), grade);
        ((MutableClock) clock).advance(Duration.ofHours(1));
        assertThat(solutions.grade(submission.id(), grade)).isEqualTo(first);
        assertThat(solutions.get(submission.id())).isEqualTo(first);
        var revised = solutions.grade(submission.id(), new GradeRequest(10, "Принято"));
        assertThat(revised.gradedAt()).isEqualTo(clock.instant());
        assertThat(revised.feedback()).isEqualTo("Принято");
        assertThat(notifications.list(student, false, Pageable.unpaged()).content())
                .extracting(NotificationResponse::type)
                .filteredOn(NotificationType.SUBMISSION_GRADED::equals)
                .hasSize(2);
    }

    @Test
    @DisplayName("Сдача в архивном курсе допускает опоздание, повторную оценку и проверяет диапазон")
    void deadline(@Autowired ApplicationEvents events) {
        var c = data.publishedCourseWithLessons(1);
        var module = c.getModules().getFirst();
        var lesson = module.getLessons().getFirst();
        var e = data.enrollment(c);
        long student = e.getStudent().getId();
        var deadline = Instant.parse("2030-01-01T00:00:00Z");
        ((MutableClock) clock).set(deadline.plusSeconds(1));
        var a = assignments.create(lesson.getId(), new AssignmentRequest("ДЗ", "Условие", deadline, 10));
        assertThat(events.stream(AssignmentCreatedEvent.class))
                .containsExactly(new AssignmentCreatedEvent(a.id(), c.getId()));
        courses.archive(c.getId());
        assertThatThrownBy(() -> solutions.submit(
                        a.id(), new SubmitRequest(data.student().getId(), "Ответ")))
                .isInstanceOf(BusinessRuleException.class);
        var s = solutions.submit(a.id(), new SubmitRequest(student, "Ответ"));
        assertThat(s.late()).isTrue();
        assertThat(s.submittedAt()).isEqualTo(clock.instant());
        assertThatThrownBy(() -> solutions.submit(a.id(), new SubmitRequest(student, "Ответ")))
                .isInstanceOf(ConflictException.class);
        solutions.grade(s.id(), new GradeRequest(0, "Исправить"));
        assertThat(solutions.grade(s.id(), new GradeRequest(10, "Принято")).score())
                .isEqualTo(10);
        assertThatThrownBy(() -> solutions.grade(s.id(), new GradeRequest(-1, null)))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(events.stream(SubmissionGradedEvent.class)).hasSize(2);
        queries.reset();
        assertThat(solutions.list(a.id())).hasSize(1);
        queries.assertCount(1);
        queries.reset();
        assertThat(solutions.studentSubmissions(student)).hasSize(1);
        queries.assertCount(1);
    }
}
