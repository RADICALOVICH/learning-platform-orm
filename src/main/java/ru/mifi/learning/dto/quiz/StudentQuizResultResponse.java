package ru.mifi.learning.dto.quiz;

import java.time.Instant;

public record StudentQuizResultResponse(
        Long id,
        Long quizId,
        String quizTitle,
        Long moduleId,
        String moduleTitle,
        Long courseId,
        String courseTitle,
        int attemptNumber,
        int percent,
        boolean passed,
        Instant takenAt) {}
