package ru.mifi.learning.dto.assignment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record AssignmentRequest(
        @NotBlank(message = "Заполните поле") @Size(max = 200, message = "Максимальная длина — 200 символов")
        String title,

        @NotBlank(message = "Заполните поле") String description,
        Instant dueDate,

        @Positive(message = "Значение должно быть положительным")
        Integer maxScore) {
    public int effectiveMaxScore() {
        return maxScore == null ? 100 : maxScore;
    }
}
