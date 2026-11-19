package ru.mifi.learning.service.event;

public record CourseCompletedEvent(Long studentId, Long courseId, Long enrollmentId) {}
