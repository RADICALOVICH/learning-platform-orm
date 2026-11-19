package ru.mifi.learning.dto.assignment;

import java.time.Instant;
import ru.mifi.learning.domain.enums.SubmissionStatus;

public record SubmissionResponse(
        Long id,
        Long assignmentId,
        Long studentId,
        String studentName,
        String content,
        Instant submittedAt,
        boolean late,
        SubmissionStatus status,
        Integer score,
        String feedback,
        Instant gradedAt) {}
