package ru.mifi.learning.dto.enrollment;

import ru.mifi.learning.domain.enums.EnrollmentStatus;

public record CourseProgressResponse(
        Long courseId,
        Long studentId,
        long completedLessons,
        long totalLessons,
        long passedQuizzes,
        long totalQuizzes,
        int percent,
        EnrollmentStatus status) {}
