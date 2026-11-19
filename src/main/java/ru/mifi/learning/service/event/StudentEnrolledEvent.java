package ru.mifi.learning.service.event;

public record StudentEnrolledEvent(Long studentId, Long courseId, Long enrollmentId) {}
