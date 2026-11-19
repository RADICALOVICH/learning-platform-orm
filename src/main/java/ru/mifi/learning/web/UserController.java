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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.mifi.learning.domain.enums.Role;
import ru.mifi.learning.dto.common.PageResponse;
import ru.mifi.learning.dto.user.CreateUserRequest;
import ru.mifi.learning.dto.user.UpdateUserRequest;
import ru.mifi.learning.dto.user.UserResponse;
import ru.mifi.learning.service.UserService;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Пользователи")
@RequiredArgsConstructor
public class UserController {
    private final UserService service;

    @PostMapping
    @Operation(summary = "Создать пользователя")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(summary = "Получить страницу пользователей")
    public PageResponse<UserResponse> list(
            @RequestParam(required = false) Role role,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return service.list(role, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить пользователя")
    public UserResponse get(@PathVariable @Positive(message = "Идентификатор должен быть положительным") long id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить пользователя")
    public UserResponse update(
            @PathVariable @Positive(message = "Идентификатор должен быть положительным") long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить пользователя")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Positive(message = "Идентификатор должен быть положительным") long id) {
        service.delete(id);
    }
}
