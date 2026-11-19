package ru.mifi.learning.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
        @NotBlank(message = "Заполните поле") @Size(max = 100, message = "Максимальная длина — 100 символов")
        String name,

        @Size(max = 500, message = "Максимальная длина — 500 символов")
        String description) {}
