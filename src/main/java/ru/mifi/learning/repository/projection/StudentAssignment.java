package ru.mifi.learning.repository.projection;

import java.time.Instant;
import ru.mifi.learning.domain.enums.SubmissionStatus;

public record StudentAssignment(
        Long assignmentId,
        String title,
        Long courseId,
        String courseTitle,
        Instant dueDate,
        Long submissionId,
        SubmissionStatus status,
        Integer score) {}
