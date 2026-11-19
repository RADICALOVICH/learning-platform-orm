package ru.mifi.learning.dto.analytics;

public record CourseAnalytics(
        long courseId,
        String courseTitle,
        long enrolledStudents,
        long completedStudents,
        Double completionPercent,
        Double averageRating,
        long reviewCount,
        Double averageAssignmentPercent,
        Double quizPassPercent) {}
