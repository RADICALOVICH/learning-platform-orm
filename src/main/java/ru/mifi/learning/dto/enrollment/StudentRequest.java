package ru.mifi.learning.dto.enrollment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StudentRequest(
        @NotNull(message = "Обязательное поле") @Positive(message = "Значение должно быть положительным")
        Long studentId) {}
