package ru.mifi.learning.dto.tag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTagRequest(
        @NotBlank(message = "Заполните поле") @Size(max = 50, message = "Максимальная длина — 50 символов")
        String name) {}
