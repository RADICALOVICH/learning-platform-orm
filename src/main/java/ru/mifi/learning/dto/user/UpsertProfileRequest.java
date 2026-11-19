package ru.mifi.learning.dto.user;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpsertProfileRequest(
        String bio,

        @Size(max = 500, message = "Максимальная длина — 500 символов")
        String avatarUrl,

        @Size(max = 30, message = "Максимальная длина — 30 символов")
        String phone,

        @Size(max = 100, message = "Максимальная длина — 100 символов")
        String city,

        @PastOrPresent(message = "Дата рождения не может быть в будущем")
        LocalDate birthDate) {}
