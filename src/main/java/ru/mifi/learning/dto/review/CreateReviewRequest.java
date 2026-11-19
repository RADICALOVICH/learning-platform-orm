package ru.mifi.learning.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateReviewRequest(
        @NotNull(message = "Обязательное поле") @Positive(message = "Значение должно быть положительным")
        Long studentId,

        @NotNull(message = "Обязательное поле")
        @Min(value = 1, message = "Минимальная оценка — 1")
        @Max(value = 5, message = "Максимальная оценка — 5")
        Integer rating,

        String comment) {}
