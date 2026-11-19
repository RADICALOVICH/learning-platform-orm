package ru.mifi.learning.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.mifi.learning.domain.enums.Role;

public record CreateUserRequest(
        @NotBlank(message = "Заполните поле")
        @Email(message = "Введите корректный email")
        @Size(max = 255, message = "Максимальная длина — 255 символов")
        String email,

        @NotBlank(message = "Заполните поле") @Size(max = 150, message = "Максимальная длина — 150 символов")
        String fullName,

        @NotNull(message = "Укажите роль") Role role) {}
