package ru.mifi.learning.dto.quiz;

import java.time.Instant;
import java.util.List;

public record QuizResultResponse(
        Long id,
        Long quizId,
        Long studentId,
        int attemptNumber,
        int score,
        int maxScore,
        int percent,
        boolean passed,
        Instant takenAt,
        List<AnswerResult> answers) {}
