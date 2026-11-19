package ru.mifi.learning.dto.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ModuleRequest(
        @NotBlank(message = "Заполните поле") @Size(max = 200, message = "Максимальная длина — 200 символов")
        String title,

        String description,

        @PositiveOrZero(message = "Порядок должен быть неотрицательным")
        Integer orderIndex) {}
