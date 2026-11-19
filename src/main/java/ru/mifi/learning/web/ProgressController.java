package ru.mifi.learning.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.mifi.learning.dto.enrollment.CourseProgressResponse;
import ru.mifi.learning.dto.enrollment.LessonProgressResponse;
import ru.mifi.learning.dto.enrollment.StudentRequest;
import ru.mifi.learning.service.ProgressService;

@RestController
@RequestMapping("/api")
@Tag(name = "Прогресс")
@RequiredArgsConstructor
public class ProgressController {
    private final ProgressService service;

    @PostMapping("/lessons/{id}/completions")
    @Operation(summary = "Отметить урок")
    @ApiResponse(responseCode = "201", description = "Первая отметка", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "200", description = "Урок уже отмечен", useReturnTypeSchema = true)
    public ResponseEntity<LessonProgressResponse> mark(
            @PathVariable @Positive(message = "Значение должно быть положительным") long id,
            @Valid @RequestBody StudentRequest request) {
        var result = service.mark(id, request);
        var body = result.progress();
        return result.created()
                ? ResponseEntity.created(
                                URI.create("/api/courses/" + body.courseId() + "/progress/" + body.studentId()))
                        .body(body)
                : ResponseEntity.ok(body);
    }

    @GetMapping("/courses/{id}/progress/{studentId}")
    @Operation(summary = "Прогресс студента")
    public CourseProgressResponse get(
            @PathVariable @Positive(message = "Значение должно быть положительным") long id,
            @PathVariable @Positive(message = "Значение должно быть положительным") long studentId) {
        return service.get(id, studentId);
    }
}
