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
import ru.mifi.learning.dto.course.ModuleRequest;
import ru.mifi.learning.dto.course.ModuleResponse;
import ru.mifi.learning.service.ModuleService;

@RestController
@RequestMapping("/api")
@Tag(name = "Модули")
@RequiredArgsConstructor
public class ModuleController {
    private final ModuleService service;

    @PostMapping("/courses/{id}/modules")
    @Operation(summary = "Создать модуль")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ModuleResponse> create(
            @PathVariable @Positive(message = "Значение должно быть положительным") long id,
            @Valid @RequestBody ModuleRequest request) {
        var result = service.create(id, request);
        return ResponseEntity.created(URI.create("/api/modules/" + result.id())).body(result);
    }

    @GetMapping("/courses/{id}/modules")
    @Operation(summary = "Список элементов")
    public List<ModuleResponse> list(@PathVariable @Positive(message = "Значение должно быть положительным") long id) {
        return service.list(id);
    }

    @GetMapping("/modules/{id}")
    @Operation(summary = "Получить модуль")
    public ModuleResponse get(@PathVariable @Positive(message = "Значение должно быть положительным") long id) {
        return service.get(id);
    }

    @PutMapping("/modules/{id}")
    @Operation(summary = "Обновить модуль")
    public ModuleResponse update(
            @PathVariable @Positive(message = "Значение должно быть положительным") long id,
            @Valid @RequestBody ModuleRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/modules/{id}")
    @Operation(summary = "Удалить модуль")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Positive(message = "Значение должно быть положительным") long id) {
        service.delete(id);
    }
}
