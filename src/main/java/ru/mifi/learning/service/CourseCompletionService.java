package ru.mifi.learning.service;

import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mifi.learning.domain.entity.Course;
import ru.mifi.learning.domain.entity.Enrollment;
import ru.mifi.learning.domain.enums.CourseStatus;
import ru.mifi.learning.domain.enums.EnrollmentStatus;
import ru.mifi.learning.dto.enrollment.CourseProgressResponse;
import ru.mifi.learning.exception.ConflictException;
import ru.mifi.learning.repository.EnrollmentRepository;
import ru.mifi.learning.repository.LessonProgressRepository;
import ru.mifi.learning.repository.LessonRepository;
import ru.mifi.learning.repository.QuizRepository;
import ru.mifi.learning.repository.QuizSubmissionRepository;
import ru.mifi.learning.service.event.CourseCompletedEvent;
import ru.mifi.learning.service.event.QuizPassedEvent;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseCompletionService {
    private final EnrollmentRepository enrollments;
    private final LessonProgressRepository progress;
    private final LessonRepository lessons;
    private final QuizRepository quizzes;
    private final QuizSubmissionRepository attempts;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    @EventListener
    @Transactional
    public void onQuizPassed(QuizPassedEvent event) {
        enrollments.lockByStudentAndCourse(event.studentId(), event.courseId()).ifPresent(this::tryComplete);
    }

    @Transactional
    public void tryComplete(Enrollment enrollment) {
        if (enrollment.getStatus() == EnrollmentStatus.COMPLETED) {
            return;
        }
        long courseId = enrollment.getCourse().getId();
        completeReady(courseId, enrollment.getId(), lessons.countByModuleCourseId(courseId));
    }

    @Transactional
    public void afterContentRemoval(Course course) {
        // Удаление дочерних строк и каскады PostgreSQL должны быть видны запросам прогресса.
        lessons.flush();
        long courseId = course.getId();
        long totalLessons = lessons.countByModuleCourseId(courseId);
        if (totalLessons == 0) {
            if (course.getStatus() == CourseStatus.PUBLISHED
                    || enrollments.existsByCourseIdAndStatus(courseId, EnrollmentStatus.ACTIVE)) {
                throw new ConflictException(
                        "COURSE_REQUIRES_LESSON",
                        "Для публикации и продолжения обучения в курсе должен остаться хотя бы один урок");
            }
            return;
        }
        // Дожидаемся параллельных отметок и сдач перед выборкой готовых к завершению записей.
        if (!enrollments.lockActiveByCourseId(courseId).isEmpty()) {
            completeReady(courseId, null, totalLessons);
        }
    }

    private void completeReady(long courseId, Long enrollmentId, long totalLessons) {
        if (totalLessons == 0) return;
        for (var enrollment : enrollments.findReadyToComplete(courseId, enrollmentId, totalLessons)) {
            enrollment.complete(clock.instant());
            events.publishEvent(new CourseCompletedEvent(
                    enrollment.getStudent().getId(), enrollment.getCourse().getId(), enrollment.getId()));
        }
    }

    public CourseProgressResponse snapshot(Enrollment enrollment) {
        long courseId = enrollment.getCourse().getId();
        long studentId = enrollment.getStudent().getId();
        long totalLessons = lessons.countByModuleCourseId(courseId);
        long completedLessons = progress.countByEnrollmentId(enrollment.getId());
        long totalQuizzes = quizzes.countByModuleCourseId(courseId);
        long passedQuizzes = attempts.countPassedQuizzes(studentId, courseId);
        int percent = totalLessons == 0 ? 0 : (int) Math.round(100.0 * completedLessons / totalLessons);
        return new CourseProgressResponse(
                courseId,
                studentId,
                completedLessons,
                totalLessons,
                passedQuizzes,
                totalQuizzes,
                percent,
                enrollment.getStatus());
    }
}
