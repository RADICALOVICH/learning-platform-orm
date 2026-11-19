package ru.mifi.learning.dto.enrollment;

import java.time.Instant;
import ru.mifi.learning.domain.enums.EnrollmentStatus;

public record EnrolledStudentResponse(
        Long studentId, String fullName, String email, EnrollmentStatus status, Instant enrolledAt) {}
