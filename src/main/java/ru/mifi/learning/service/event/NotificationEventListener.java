package ru.mifi.learning.service.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import ru.mifi.learning.domain.entity.Notification;
import ru.mifi.learning.domain.enums.EnrollmentStatus;
import ru.mifi.learning.domain.enums.NotificationType;
import ru.mifi.learning.repository.EnrollmentRepository;
import ru.mifi.learning.repository.NotificationRepository;
import ru.mifi.learning.repository.UserRepository;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {
    private final NotificationRepository notifications;
    private final EnrollmentRepository enrollments;
    private final UserRepository users;
    private final ApplicationEventPublisher events;

    @EventListener
    public void onEnrollment(StudentEnrolledEvent event) {
        notifyStudent(
                event.studentId(),
                NotificationType.COURSE_ENROLLED,
                "Запись на курс",
                "Вы записаны на курс №" + event.courseId());
    }

    @EventListener
    public void onAssignment(AssignmentCreatedEvent event) {
        var recipients = enrollments.findStudentIdsByCourseIdAndStatus(event.courseId(), EnrollmentStatus.ACTIVE);
        for (long student : recipients) {
            notifyStudent(
                    student,
                    NotificationType.NEW_ASSIGNMENT,
                    "Новое задание",
                    "В курсе №%d опубликовано задание №%d".formatted(event.courseId(), event.assignmentId()));
        }
    }

    @EventListener
    public void onGrade(SubmissionGradedEvent event) {
        notifyStudent(
                event.studentId(),
                NotificationType.SUBMISSION_GRADED,
                "Решение оценено",
                "Выставлена оценка за решение №" + event.submissionId());
    }

    @EventListener
    public void onCompletion(CourseCompletedEvent event) {
        notifyStudent(
                event.studentId(),
                NotificationType.COURSE_COMPLETED,
                "Курс завершён",
                "Вы завершили курс №" + event.courseId());
    }

    private void notifyStudent(long recipient, NotificationType type, String title, String message) {
        var saved = notifications.save(new Notification(users.getReferenceById(recipient), type, title, message));
        events.publishEvent(new NotificationCreatedEvent(saved.getId(), recipient));
    }
}
