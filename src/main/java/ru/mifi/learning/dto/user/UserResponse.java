package ru.mifi.learning.dto.user;

import ru.mifi.learning.domain.enums.Role;

public record UserResponse(Long id, String email, String fullName, Role role) {}
