package ru.mifi.learning.dto.enrollment;

import java.time.Instant;
import ru.mifi.learning.domain.enums.EnrollmentStatus;

public record StudentCourseResponse(
        Long courseId, String title, Long teacherId, String teacherName, EnrollmentStatus status, Instant enrolledAt) {}
