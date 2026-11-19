package ru.mifi.learning.service;

import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mifi.learning.domain.entity.Enrollment;
import ru.mifi.learning.domain.enums.EnrollmentStatus;
import ru.mifi.learning.dto.enrollment.CourseProgressResponse;
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
        var snapshot = snapshot(enrollment);
        if (snapshot.completedLessons() == snapshot.totalLessons()
                && snapshot.passedQuizzes() == snapshot.totalQuizzes()) {
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
