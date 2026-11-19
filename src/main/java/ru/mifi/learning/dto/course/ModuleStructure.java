package ru.mifi.learning.dto.course;

import java.util.List;

public record ModuleStructure(Long id, String title, int orderIndex, Long quizId, List<LessonSummary> lessons) {}
