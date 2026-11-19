package ru.mifi.learning.dto.quiz;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record TakeQuizRequest(
        @NotNull(message = "Обязательное поле") @Positive(message = "Значение должно быть положительным")
        Long studentId,

        @NotNull(message = "Обязательное поле")
        List<@NotNull(message = "Обязательное поле") @Valid QuizAnswerRequest> answers) {}
