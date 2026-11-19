package ru.mifi.learning.service.event;

public record SubmissionGradedEvent(Long submissionId, Long studentId, Long assignmentId) {}
