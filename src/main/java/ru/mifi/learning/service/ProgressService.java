package ru.mifi.learning.service;

import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mifi.learning.domain.entity.LessonProgress;
import ru.mifi.learning.domain.entity.LessonProgressId;
import ru.mifi.learning.dto.enrollment.CourseProgressResponse;
import ru.mifi.learning.dto.enrollment.LessonProgressResponse;
import ru.mifi.learning.dto.enrollment.StudentRequest;
import ru.mifi.learning.exception.BusinessRuleException;
import ru.mifi.learning.exception.NotFoundException;
import ru.mifi.learning.repository.EnrollmentRepository;
import ru.mifi.learning.repository.LessonProgressRepository;
import ru.mifi.learning.repository.LessonRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProgressService {
    private final EnrollmentRepository enrollments;
    private final LessonRepository lessons;
    private final LessonProgressRepository progress;
    private final CourseCompletionService completion;
    private final Clock clock;

    public record MarkResult(boolean created, LessonProgressResponse progress) {}

    @Transactional
    public MarkResult mark(long id, StudentRequest request) {
        long course = lessons.findCourseIdById(id).orElseThrow(() -> NotFoundException.of("Lesson", id));
        var enrollment = enrollments
                .lockByStudentAndCourse(request.studentId(), course)
                .orElseThrow(() ->
                        new BusinessRuleException("LESSON_NOT_IN_COURSE", "Студент не записан на курс этого урока"));
        var key = new LessonProgressId(enrollment.getId(), id);
        var existing = progress.findById(key);
        var entry = existing.orElseGet(
                () -> progress.save(new LessonProgress(enrollment, lessons.getReferenceById(id), clock.instant())));
        completion.tryComplete(enrollment);
        return new MarkResult(
                existing.isEmpty(),
                new LessonProgressResponse(id, request.studentId(), course, entry.getCompletedAt()));
    }

    public CourseProgressResponse get(long id, long studentId) {
        return completion.snapshot(enrollments
                .findByStudentIdAndCourseId(studentId, id)
                .orElseThrow(() -> NotFoundException.enrollment(id, studentId)));
    }
}
