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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.mifi.learning.dto.category.CategoryResponse;
import ru.mifi.learning.dto.category.CreateCategoryRequest;
import ru.mifi.learning.dto.category.UpdateCategoryRequest;
import ru.mifi.learning.service.CategoryService;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Категории")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService service;

    @PostMapping
    @Operation(summary = "Создать категорию")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CreateCategoryRequest request) {
        CategoryResponse response = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(summary = "Получить список категорий")
    public List<CategoryResponse> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить категорию")
    public CategoryResponse get(@PathVariable @Positive(message = "Идентификатор должен быть положительным") long id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить категорию")
    public CategoryResponse update(
            @PathVariable @Positive(message = "Идентификатор должен быть положительным") long id,
            @Valid @RequestBody UpdateCategoryRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить категорию")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Positive(message = "Идентификатор должен быть положительным") long id) {
        service.delete(id);
    }
}
