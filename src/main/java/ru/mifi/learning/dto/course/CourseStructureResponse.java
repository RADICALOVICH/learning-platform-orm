package ru.mifi.learning.dto.course;

import java.util.List;

public record CourseStructureResponse(Long id, String title, List<ModuleStructure> modules) {}
