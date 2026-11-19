package ru.mifi.learning.dto.assignment;

import java.time.Instant;

public record AssignmentResponse(
        Long id, Long lessonId, String title, String description, Instant dueDate, int maxScore) {}
