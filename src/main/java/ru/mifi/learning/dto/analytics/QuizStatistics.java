package ru.mifi.learning.dto.analytics;

public record QuizStatistics(
        long quizId,
        String title,
        long attempts,
        long uniqueStudents,
        Double averageScorePercent,
        Double passPercent) {}
