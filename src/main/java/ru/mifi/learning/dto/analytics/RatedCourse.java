package ru.mifi.learning.dto.analytics;

public record RatedCourse(long courseId, String courseTitle, double averageRating, long reviewCount) {}
