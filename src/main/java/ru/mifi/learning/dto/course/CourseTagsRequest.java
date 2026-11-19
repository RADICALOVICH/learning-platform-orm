package ru.mifi.learning.dto.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CourseTagsRequest(
        @NotNull(message = "Обязательное поле")
        List<@NotBlank(message = "Заполните поле") @Size(max = 50, message = "Максимальная длина — 50 символов") String>
                names) {}
