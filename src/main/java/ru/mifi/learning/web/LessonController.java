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
import ru.mifi.learning.dto.course.LessonRequest;
import ru.mifi.learning.dto.course.LessonResponse;
import ru.mifi.learning.service.LessonService;

@RestController
@RequestMapping("/api")
@Tag(name = "Уроки")
@RequiredArgsConstructor
public class LessonController {
    private final LessonService service;

    @PostMapping("/modules/{id}/lessons")
    @Operation(summary = "Создать урок")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<LessonResponse> create(
            @PathVariable @Positive(message = "Значение должно быть положительным") long id,
            @Valid @RequestBody LessonRequest request) {
        var result = service.create(id, request);
        return ResponseEntity.created(URI.create("/api/lessons/" + result.id())).body(result);
    }

    @GetMapping("/modules/{id}/lessons")
    @Operation(summary = "Список элементов")
    public List<LessonResponse> list(@PathVariable @Positive(message = "Значение должно быть положительным") long id) {
        return service.list(id);
    }

    @GetMapping("/lessons/{id}")
    @Operation(summary = "Получить урок")
    public LessonResponse get(@PathVariable @Positive(message = "Значение должно быть положительным") long id) {
        return service.get(id);
    }

    @PutMapping("/lessons/{id}")
    @Operation(summary = "Обновить урок")
    public LessonResponse update(
            @PathVariable @Positive(message = "Значение должно быть положительным") long id,
            @Valid @RequestBody LessonRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/lessons/{id}")
    @Operation(summary = "Удалить урок")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Positive(message = "Значение должно быть положительным") long id) {
        service.delete(id);
    }
}
