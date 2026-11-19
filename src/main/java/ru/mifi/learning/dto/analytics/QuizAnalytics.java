package ru.mifi.learning.dto.analytics;

import java.util.List;

public record QuizAnalytics(QuizStatistics statistics, List<QuestionAnalytics> questions, Long hardestQuestionId) {}
