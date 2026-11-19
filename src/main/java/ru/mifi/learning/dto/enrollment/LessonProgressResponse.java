package ru.mifi.learning.dto.enrollment;

import java.time.Instant;

public record LessonProgressResponse(Long lessonId, Long studentId, Long courseId, Instant completedAt) {}
