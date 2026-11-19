package ru.mifi.learning.dto.course;

import java.util.List;

public record ModuleResponse(
        Long id, Long courseId, String title, String description, int orderIndex, List<LessonResponse> lessons) {}
