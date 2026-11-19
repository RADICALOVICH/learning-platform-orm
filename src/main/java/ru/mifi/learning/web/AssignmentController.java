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
import ru.mifi.learning.dto.assignment.AssignmentRequest;
import ru.mifi.learning.dto.assignment.AssignmentResponse;
import ru.mifi.learning.service.AssignmentService;

@RestController
@RequestMapping("/api")
@Tag(name = "Задания")
@RequiredArgsConstructor
public class AssignmentController {
    private final AssignmentService service;

    @PostMapping("/lessons/{id}/assignments")
    @Operation(summary = "Создать задание")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<AssignmentResponse> create(
            @PathVariable @Positive(message = "Значение должно быть положительным") long id,
            @Valid @RequestBody AssignmentRequest request) {
        var result = service.create(id, request);
        return ResponseEntity.created(URI.create("/api/assignments/" + result.id()))
                .body(result);
    }

    @GetMapping("/lessons/{id}/assignments")
    @Operation(summary = "Задания урока")
    public List<AssignmentResponse> list(
            @PathVariable @Positive(message = "Значение должно быть положительным") long id) {
        return service.list(id);
    }

    @GetMapping("/assignments/{id}")
    @Operation(summary = "Получить задание")
    public AssignmentResponse get(@PathVariable @Positive(message = "Значение должно быть положительным") long id) {
        return service.get(id);
    }

    @PutMapping("/assignments/{id}")
    @Operation(summary = "Обновить задание")
    public AssignmentResponse update(
            @PathVariable @Positive(message = "Значение должно быть положительным") long id,
            @Valid @RequestBody AssignmentRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/assignments/{id}")
    @Operation(summary = "Удалить задание")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Positive(message = "Значение должно быть положительным") long id) {
        service.delete(id);
    }
}
