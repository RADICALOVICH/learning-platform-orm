package ru.mifi.learning.dto.assignment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SubmitRequest(
        @NotNull(message = "Обязательное поле") @Positive(message = "Значение должно быть положительным")
        Long studentId,

        @NotBlank(message = "Заполните поле") String content) {}
