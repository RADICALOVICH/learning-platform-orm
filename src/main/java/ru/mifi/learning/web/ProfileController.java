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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.mifi.learning.dto.user.ProfileResponse;
import ru.mifi.learning.dto.user.UpsertProfileRequest;
import ru.mifi.learning.service.ProfileService;

@RestController
@RequestMapping("/api/users/{id}/profile")
@Tag(name = "Профили")
@RequiredArgsConstructor
public class ProfileController {
    private final ProfileService service;

    @GetMapping
    @Operation(summary = "Получить профиль пользователя")
    public ProfileResponse get(@PathVariable @Positive(message = "Идентификатор должен быть положительным") long id) {
        return service.get(id);
    }

    @PutMapping
    @Operation(summary = "Создать или обновить профиль")
    @ApiResponse(responseCode = "201", description = "Профиль создан", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "200", description = "Профиль обновлён", useReturnTypeSchema = true)
    public ResponseEntity<ProfileResponse> upsert(
            @PathVariable @Positive(message = "Идентификатор должен быть положительным") long id,
            @Valid @RequestBody UpsertProfileRequest request) {
        var result = service.upsert(id, request);
        return result.created()
                ? ResponseEntity.created(URI.create("/api/users/" + id + "/profile"))
                        .body(result.profile())
                : ResponseEntity.ok(result.profile());
    }
}
