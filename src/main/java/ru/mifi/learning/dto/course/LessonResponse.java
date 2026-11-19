package ru.mifi.learning.dto.course;

public record LessonResponse(
        Long id,
        Long moduleId,
        String title,
        String content,
        String videoUrl,
        Integer durationMinutes,
        int orderIndex) {}
