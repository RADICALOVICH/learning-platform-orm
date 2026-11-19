package ru.mifi.learning.dto.assignment;

import ru.mifi.learning.domain.enums.SubmissionStatus;

public record StudentSubmissionResponse(
        Long id,
        Long assignmentId,
        String assignmentTitle,
        Long courseId,
        String courseTitle,
        Integer score,
        SubmissionStatus status) {}
