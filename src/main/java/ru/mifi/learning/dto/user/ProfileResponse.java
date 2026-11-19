package ru.mifi.learning.dto.user;

import java.time.LocalDate;

public record ProfileResponse(
        Long userId, String bio, String avatarUrl, String phone, String city, LocalDate birthDate) {}
