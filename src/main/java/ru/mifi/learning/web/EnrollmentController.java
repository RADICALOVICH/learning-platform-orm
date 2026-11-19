package ru.mifi.learning.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.mifi.learning.dto.common.PageResponse;
import ru.mifi.learning.dto.enrollment.EnrolledStudentResponse;
import ru.mifi.learning.dto.enrollment.EnrollmentResponse;
import ru.mifi.learning.dto.enrollment.StudentRequest;
import ru.mifi.learning.service.EnrollmentService;

@RestController
@RequestMapping("/api")
@Tag(name = "Запись на курс")
@RequiredArgsConstructor
public class EnrollmentController {
    private final EnrollmentService service;

    @PostMapping("/courses/{id}/enrollments")
    @Operation(summary = "Записать студента")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<EnrollmentResponse> enroll(
            @PathVariable @Positive(message = "Значение должно быть положительным") long id,
            @Valid @RequestBody StudentRequest request) {
        var e = service.enroll(id, request);
        return ResponseEntity.created(URI.create("/api/courses/" + id + "/enrollments/" + e.studentId()))
                .body(e);
    }

    @GetMapping("/courses/{id}/enrollments")
    @Operation(summary = "Студенты курса")
    public PageResponse<EnrolledStudentResponse> students(
            @PathVariable @Positive(message = "Значение должно быть положительным") long id,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return service.students(id, pageable);
    }

    @DeleteMapping("/courses/{id}/enrollments/{studentId}")
    @Operation(summary = "Отписать студента")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsubscribe(
            @PathVariable @Positive(message = "Значение должно быть положительным") long id,
            @PathVariable @Positive(message = "Значение должно быть положительным") long studentId) {
        service.unsubscribe(id, studentId);
    }
}
