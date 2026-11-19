package ru.mifi.learning.dto.quiz;

import java.time.Instant;

public record QuizSubmissionSummary(
        Long id, Long studentId, String studentName, int attemptNumber, int percent, boolean passed, Instant takenAt) {}
