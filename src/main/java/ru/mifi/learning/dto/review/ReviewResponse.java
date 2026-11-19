package ru.mifi.learning.dto.review;

public record ReviewResponse(Long id, Long courseId, Long studentId, String studentName, int rating, String comment) {}
