package ru.mifi.learning.dto.quiz;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record QuizAnswerRequest(
        @NotNull(message = "Обязательное поле") @Positive(message = "Значение должно быть положительным")
        Long questionId,

        @NotNull(message = "Обязательное поле")
        List<@NotNull(message = "Обязательное поле") @Positive(message = "Значение должно быть положительным") Long>
                optionIds) {}
