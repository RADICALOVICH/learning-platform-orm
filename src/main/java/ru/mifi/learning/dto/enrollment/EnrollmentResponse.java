package ru.mifi.learning.dto.enrollment;

import java.time.Instant;
import ru.mifi.learning.domain.enums.EnrollmentStatus;

public record EnrollmentResponse(
        Long id, Long studentId, Long courseId, EnrollmentStatus status, Instant enrolledAt, Instant completedAt) {}
