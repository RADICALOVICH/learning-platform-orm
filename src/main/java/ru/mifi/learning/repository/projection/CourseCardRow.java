package ru.mifi.learning.repository.projection;

import ru.mifi.learning.domain.entity.Course;

public record CourseCardRow(Course course, String tag, long modules, long students, Double rating) {}
