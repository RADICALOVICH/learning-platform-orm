package ru.mifi.learning.dto.quiz;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateQuizRequest(
        @NotBlank(message = "Заполните поле") @Size(max = 200, message = "Максимальная длина — 200 символов")
        String title,

        String description,

        @Positive(message = "Значение должно быть положительным")
        Integer timeLimitMinutes,

        @Min(value = 0, message = "Минимальный процент — 0") @Max(value = 100, message = "Максимальный процент — 100")
        Integer passingScorePercent,

        @Positive(message = "Значение должно быть положительным")
        Integer maxAttempts,

        @NotNull(message = "Обязательное поле")
        List<@NotNull(message = "Обязательное поле") @Valid QuestionRequest> questions) {}
