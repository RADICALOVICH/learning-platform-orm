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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.mifi.learning.dto.tag.CreateTagRequest;
import ru.mifi.learning.dto.tag.TagResponse;
import ru.mifi.learning.service.TagService;

@RestController
@RequestMapping("/api/tags")
@Tag(name = "Теги")
@RequiredArgsConstructor
public class TagController {
    private final TagService service;

    @PostMapping
    @Operation(summary = "Создать тег")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<TagResponse> create(@Valid @RequestBody CreateTagRequest request) {
        TagResponse response = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(summary = "Получить список тегов")
    public List<TagResponse> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить тег")
    public TagResponse get(@PathVariable @Positive(message = "Идентификатор должен быть положительным") long id) {
        return service.get(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить тег")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Positive(message = "Идентификатор должен быть положительным") long id) {
        service.delete(id);
    }
}
