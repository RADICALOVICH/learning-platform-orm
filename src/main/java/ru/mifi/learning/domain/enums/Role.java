package ru.mifi.learning.domain.enums;

public enum Role {
    STUDENT,
    TEACHER,
    ADMIN;

    public boolean canTeach() {
        return this == TEACHER || this == ADMIN;
    }
}
