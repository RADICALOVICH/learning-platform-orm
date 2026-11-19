package ru.mifi.learning.dto.quiz;

import java.util.List;

public record AnswerResult(Long questionId, List<Long> optionIds, boolean correct, int pointsAwarded) {}
