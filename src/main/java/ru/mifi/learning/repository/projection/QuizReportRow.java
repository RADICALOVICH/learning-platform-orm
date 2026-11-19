package ru.mifi.learning.repository.projection;

import ru.mifi.learning.dto.analytics.QuizStatistics;

public record QuizReportRow(
        QuizStatistics statistics, Long questionId, String text, long answers, Double correctPercent) {}
