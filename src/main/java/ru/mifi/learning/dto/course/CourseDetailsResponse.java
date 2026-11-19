package ru.mifi.learning.dto.course;

import java.time.LocalDate;
import java.util.List;
import ru.mifi.learning.domain.enums.CourseStatus;
import ru.mifi.learning.dto.category.CategoryResponse;
import ru.mifi.learning.dto.user.UserResponse;

public record CourseDetailsResponse(
        Long id,
        String title,
        String description,
        CourseStatus status,
        LocalDate startDate,
        Integer durationWeeks,
        UserResponse teacher,
        CategoryResponse category,
        List<String> tags,
        long moduleCount,
        long studentCount,
        Double averageRating) {}
