package ru.mifi.learning.dto.course;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record CreateCourseRequest(
        @NotBlank(message = "Заполните поле") @Size(max = 200, message = "Максимальная длина — 200 символов")
        String title,

        String description,

        @NotNull(message = "Обязательное поле") @Positive(message = "Значение должно быть положительным")
        Long categoryId,

        @NotNull(message = "Обязательное поле") @Positive(message = "Значение должно быть положительным")
        Long teacherId,

        LocalDate startDate,

        @Positive(message = "Значение должно быть положительным")
        Integer durationWeeks,

        List<@NotBlank(message = "Заполните поле") @Size(max = 50, message = "Максимальная длина — 50 символов") String>
                tags,
        List<@NotNull(message = "Обязательное поле") @Valid ModuleInput> modules) {}
