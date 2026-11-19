package ru.mifi.learning.dto.quiz;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OptionRequest(
        @NotBlank(message = "Заполните поле") @Size(max = 500, message = "Максимальная длина — 500 символов")
        String text,

        @NotNull(message = "Обязательное поле") Boolean correct) {}
