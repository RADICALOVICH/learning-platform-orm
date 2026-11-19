package ru.mifi.learning.dto.analytics;

public record QuestionAnalytics(long questionId, String text, long answers, Double correctPercent) {}
