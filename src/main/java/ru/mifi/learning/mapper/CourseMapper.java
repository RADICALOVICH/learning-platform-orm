package ru.mifi.learning.mapper;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;
import ru.mifi.learning.domain.entity.Course;
import ru.mifi.learning.domain.entity.CourseModule;
import ru.mifi.learning.domain.entity.Lesson;
import ru.mifi.learning.domain.entity.Tag;
import ru.mifi.learning.dto.category.CategoryResponse;
import ru.mifi.learning.dto.course.CourseDetailsResponse;
import ru.mifi.learning.dto.course.CourseStructureResponse;
import ru.mifi.learning.dto.course.CourseSummaryResponse;
import ru.mifi.learning.dto.course.LessonResponse;
import ru.mifi.learning.dto.course.LessonSummary;
import ru.mifi.learning.dto.course.ModuleResponse;
import ru.mifi.learning.dto.course.ModuleStructure;
import ru.mifi.learning.dto.user.UserResponse;
import ru.mifi.learning.repository.projection.CourseCardRow;

@Component
public class CourseMapper {
    public CourseSummaryResponse summary(Course course) {
        return new CourseSummaryResponse(
                course.getId(),
                course.getTitle(),
                course.getStatus(),
                course.getTeacher().getId(),
                course.getTeacher().getFullName(),
                course.getCategory().getId(),
                course.getCategory().getName(),
                course.getTags().stream().map(Tag::getName).sorted().toList());
    }

    public CourseDetailsResponse details(List<CourseCardRow> rows) {
        var row = rows.getFirst();
        var course = row.course();
        var teacher = course.getTeacher();
        var category = course.getCategory();
        return new CourseDetailsResponse(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getStatus(),
                course.getStartDate(),
                course.getDurationWeeks(),
                new UserResponse(teacher.getId(), teacher.getEmail(), teacher.getFullName(), teacher.getRole()),
                new CategoryResponse(category.getId(), category.getName(), category.getDescription()),
                rows.stream().map(CourseCardRow::tag).filter(Objects::nonNull).toList(),
                row.modules(),
                row.students(),
                row.rating());
    }

    public CourseStructureResponse structure(Course course, Map<Long, Long> quizzes) {
        return new CourseStructureResponse(
                course.getId(),
                course.getTitle(),
                course.getModules().stream()
                        .map(module -> new ModuleStructure(
                                module.getId(),
                                module.getTitle(),
                                module.getOrderIndex(),
                                quizzes.get(module.getId()),
                                module.getLessons().stream()
                                        .map(lesson -> new LessonSummary(
                                                lesson.getId(), lesson.getTitle(), lesson.getOrderIndex()))
                                        .toList()))
                        .toList());
    }

    public ModuleResponse module(CourseModule module) {
        return new ModuleResponse(
                module.getId(),
                module.getCourse().getId(),
                module.getTitle(),
                module.getDescription(),
                module.getOrderIndex(),
                module.getLessons().stream().map(this::lesson).toList());
    }

    public LessonResponse lesson(Lesson lesson) {
        return new LessonResponse(
                lesson.getId(),
                lesson.getModule().getId(),
                lesson.getTitle(),
                lesson.getContent(),
                lesson.getVideoUrl(),
                lesson.getDurationMinutes(),
                lesson.getOrderIndex());
    }
}
