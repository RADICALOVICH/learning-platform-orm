package ru.mifi.learning.dto.assignment;

import jakarta.validation.constraints.NotNull;

public record GradeRequest(
        @NotNull(message = "Обязательное поле") Integer score, String feedback) {}
