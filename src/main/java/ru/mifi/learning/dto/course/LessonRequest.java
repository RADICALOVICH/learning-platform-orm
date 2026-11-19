package ru.mifi.learning.dto.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record LessonRequest(
        @NotBlank(message = "Заполните поле") @Size(max = 200, message = "Максимальная длина — 200 символов")
        String title,

        String content,

        @Size(max = 500, message = "Максимальная длина — 500 символов")
        String videoUrl,

        @Positive(message = "Значение должно быть положительным")
        Integer durationMinutes,

        @PositiveOrZero(message = "Порядок должен быть неотрицательным")
        Integer orderIndex) {}
