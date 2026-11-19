package ru.mifi.learning.service.event;

public record AssignmentCreatedEvent(Long assignmentId, Long courseId) {}
