package ru.mifi.learning.dto.analytics;

public record StudentCourseAnalytics(
        long courseId,
        String courseTitle,
        double lessonCompletionPercent,
        Double averageAssignmentPercent,
        Integer bestQuizPercent,
        long passedQuizzes) {}
