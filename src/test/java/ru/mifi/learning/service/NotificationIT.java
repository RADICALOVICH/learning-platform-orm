package ru.mifi.learning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;
import ru.mifi.learning.domain.enums.NotificationType;
import ru.mifi.learning.dto.assignment.AssignmentRequest;
import ru.mifi.learning.dto.enrollment.StudentRequest;
import ru.mifi.learning.dto.notification.NotificationResponse;
import ru.mifi.learning.exception.NotFoundException;
import ru.mifi.learning.repository.NotificationRepository;
import ru.mifi.learning.service.event.EmailStubListener;
import ru.mifi.learning.support.AbstractIntegrationTest;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
class NotificationIT extends AbstractIntegrationTest {
    private final EnrollmentService enrollments;
    private final AssignmentService assignments;
    private final ProgressService progress;
    private final NotificationService service;
    private final NotificationRepository notifications;
    private final EntityManager em;
    private final TransactionTemplate tx;

    @MockitoSpyBean
    EmailStubListener email;

    @Test
    @DisplayName("Запись и уведомление атомарны, email вызывается только после коммита")
    void rollback() {
        long course = data.publishedCourseWithLessons(1).getId();
        var student = new StudentRequest(data.student().getId());
        tx.executeWithoutResult(s -> {
            enrollments.enroll(course, student);
            assertThat(notifications.count()).isOne();
            verify(email, never()).onNotification(any());
            s.setRollbackOnly();
        });
        assertThat(notifications.count()).isZero();
        verify(email, never()).onNotification(any());
        enrollments.enroll(course, student);
        verify(email).onNotification(any());
        assertThat(service.list(student.studentId(), true, Pageable.unpaged()).content())
                .extracting(NotificationResponse::type)
                .containsExactly(NotificationType.COURSE_ENROLLED);
    }

    @Test
    @DisplayName("О новом задании узнают только активные студенты этого курса")
    void activeRecipients() {
        var c = data.publishedCourseWithLessons(1);
        long lesson = c.getModules().getFirst().getLessons().getFirst().getId();
        var active = data.enrollment(c).getStudent().getId();
        var second = data.enrollment(c).getStudent().getId();
        var completed = data.enrollment(c).getStudent().getId();
        var outside =
                data.enrollment(data.publishedCourseWithLessons(1)).getStudent().getId();
        progress.mark(lesson, new StudentRequest(completed));
        assignments.create(lesson, new AssignmentRequest("Практика", "Решите задачу", null, 10));
        for (long id : new long[] {active, second}) {
            assertThat(service.list(id, true, Pageable.unpaged()).content())
                    .extracting(NotificationResponse::type)
                    .containsExactly(NotificationType.NEW_ASSIGNMENT);
        }
        assertThat(service.list(completed, true, Pageable.unpaged()).content())
                .extracting(NotificationResponse::type)
                .containsExactly(NotificationType.COURSE_COMPLETED);
        assertThat(service.list(outside, true, Pageable.unpaged()).content()).isEmpty();
    }

    @Test
    @DisplayName("Прочитать все меняет только непрочитанные строки получателя одним UPDATE и очищает контекст")
    void readAll() {
        var c = data.publishedCourseWithLessons(1);
        long student = data.student().getId();
        long other = data.student().getId();
        enrollments.enroll(c.getId(), new StudentRequest(student));
        enrollments.enroll(c.getId(), new StudentRequest(other));
        long lesson = c.getModules().getFirst().getLessons().getFirst().getId();
        assignments.create(lesson, new AssignmentRequest("А", "А", null, 1));
        assignments.create(lesson, new AssignmentRequest("Б", "Б", null, 1));
        var items = service.list(student, true, Pageable.unpaged()).content();
        service.read(items.getFirst().id());
        tx.executeWithoutResult(s -> {
            long id = items.getLast().id();
            var before = notifications.findById(id).orElseThrow();
            assertThat(before.isRead()).isFalse();
            queries.reset();
            assertThat(service.readAll(student).updated()).isEqualTo(2);
            queries.assertCount(1);
            assertThat(em.contains(before)).isFalse();
            assertThat(notifications.findById(id).orElseThrow().isRead()).isTrue();
        });
        assertThat(service.list(student, true, Pageable.unpaged()).totalElements())
                .isZero();
        assertThat(service.list(other, true, Pageable.unpaged()).totalElements())
                .isEqualTo(3);
        assertThat(service.readAll(student).updated()).isZero();
    }

    @Test
    @DisplayName("Отсутствующие получатель и уведомление дают предметный 404")
    void missing() {
        assertThatThrownBy(() -> service.readAll(Long.MAX_VALUE)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.list(Long.MAX_VALUE, true, Pageable.unpaged()))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.read(Long.MAX_VALUE)).isInstanceOf(NotFoundException.class);
    }
}
