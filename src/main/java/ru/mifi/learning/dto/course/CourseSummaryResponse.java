package ru.mifi.learning.dto.course;

import java.util.List;
import ru.mifi.learning.domain.enums.CourseStatus;

public record CourseSummaryResponse(
        Long id,
        String title,
        CourseStatus status,
        Long teacherId,
        String teacherName,
        Long categoryId,
        String categoryName,
        List<String> tags) {}
