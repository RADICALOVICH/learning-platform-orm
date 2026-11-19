package ru.mifi.learning.dto.course;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ModuleInput(
        @NotBlank(message = "Заполните поле") @Size(max = 200, message = "Максимальная длина — 200 символов")
        String title,

        String description,

        @PositiveOrZero(message = "Порядок должен быть неотрицательным")
        Integer orderIndex,

        List<@NotNull(message = "Обязательное поле") @Valid LessonRequest> lessons) {}
