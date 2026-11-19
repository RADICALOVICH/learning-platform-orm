package ru.mifi.learning.dto.quiz;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import ru.mifi.learning.domain.enums.QuestionType;

public record QuestionRequest(
        @NotBlank(message = "Заполните поле") String text,
        @NotNull(message = "Обязательное поле") QuestionType type,

        @Positive(message = "Значение должно быть положительным")
        Integer points,

        @NotNull(message = "Обязательное поле")
        List<@NotNull(message = "Обязательное поле") @Valid OptionRequest> options) {}
