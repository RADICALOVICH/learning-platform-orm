package ru.mifi.learning.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.mifi.learning.dto.common.PageResponse;
import ru.mifi.learning.dto.notification.NotificationResponse;
import ru.mifi.learning.dto.notification.ReadAllResponse;
import ru.mifi.learning.service.NotificationService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "Уведомления")
public class NotificationController {
    private final NotificationService service;

    @GetMapping("/users/{id}/notifications")
    @Operation(summary = "Уведомления пользователя")
    public PageResponse<NotificationResponse> list(
            @PathVariable @Positive(message = "Значение должно быть положительным") long id,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @ParameterObject @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return service.list(id, unreadOnly, pageable);
    }

    @PatchMapping("/notifications/{id}/read")
    @Operation(summary = "Прочитать уведомление")
    public NotificationResponse read(@PathVariable @Positive(message = "Значение должно быть положительным") long id) {
        return service.read(id);
    }

    @PostMapping("/users/{id}/notifications/read-all")
    @Operation(summary = "Прочитать все уведомления пользователя")
    public ReadAllResponse readAll(@PathVariable @Positive(message = "Значение должно быть положительным") long id) {
        return service.readAll(id);
    }
}
