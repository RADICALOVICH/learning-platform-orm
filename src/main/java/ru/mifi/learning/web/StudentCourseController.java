package ru.mifi.learning.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.mifi.learning.dto.course.CourseSummaryResponse;
import ru.mifi.learning.dto.enrollment.StudentCourseResponse;
import ru.mifi.learning.service.EnrollmentService;

@RestController
@RequestMapping("/api")
@Tag(name = "Курсы пользователя")
@RequiredArgsConstructor
public class StudentCourseController {
    private final EnrollmentService service;

    @GetMapping("/users/{id}/courses")
    @Operation(summary = "Курсы студента")
    public List<StudentCourseResponse> studentCourses(
            @PathVariable @Positive(message = "Значение должно быть положительным") long id) {
        return service.studentCourses(id);
    }

    @GetMapping("/users/{id}/courses-taught")
    @Operation(summary = "Курсы преподавателя")
    public List<CourseSummaryResponse> taught(
            @PathVariable @Positive(message = "Значение должно быть положительным") long id) {
        return service.taught(id);
    }
}
