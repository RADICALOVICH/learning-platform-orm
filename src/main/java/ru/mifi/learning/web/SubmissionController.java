package ru.mifi.learning.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.mifi.learning.dto.assignment.GradeRequest;
import ru.mifi.learning.dto.assignment.StudentSubmissionResponse;
import ru.mifi.learning.dto.assignment.SubmissionResponse;
import ru.mifi.learning.dto.assignment.SubmitRequest;
import ru.mifi.learning.service.SubmissionService;

@RestController
@RequestMapping("/api")
@Tag(name = "Решения")
@RequiredArgsConstructor
public class SubmissionController {
    private final SubmissionService service;

    @PostMapping("/assignments/{id}/submissions")
    @Operation(summary = "Сдать решение")
    public ResponseEntity<SubmissionResponse> submit(
            @PathVariable @Positive(message = "Значение должно быть положительным") long id,
            @Valid @RequestBody SubmitRequest request) {
        var result = service.submit(id, request);
        return ResponseEntity.created(URI.create("/api/submissions/" + result.id()))
                .body(result);
    }

    @GetMapping("/assignments/{id}/submissions")
    @Operation(summary = "Решения задания")
    public List<SubmissionResponse> list(
            @PathVariable @Positive(message = "Значение должно быть положительным") long id) {
        return service.list(id);
    }

    @GetMapping("/submissions/{id}")
    @Operation(summary = "Получить решение")
    public SubmissionResponse get(@PathVariable @Positive(message = "Значение должно быть положительным") long id) {
        return service.get(id);
    }

    @PutMapping("/submissions/{id}/grade")
    @Operation(summary = "Оценить решение")
    public SubmissionResponse grade(
            @PathVariable @Positive(message = "Значение должно быть положительным") long id,
            @Valid @RequestBody GradeRequest request) {
        return service.grade(id, request);
    }

    @DeleteMapping("/submissions/{id}")
    @Operation(summary = "Сбросить решение")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Positive(message = "Значение должно быть положительным") long id) {
        service.delete(id);
    }

    @GetMapping("/users/{id}/submissions")
    @Operation(summary = "Решения студента")
    public List<StudentSubmissionResponse> studentSubmissions(
            @PathVariable @Positive(message = "Идентификатор должен быть положительным") long id) {
        return service.studentSubmissions(id);
    }
}
