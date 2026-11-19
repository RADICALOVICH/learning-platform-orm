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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.mifi.learning.dto.common.PageResponse;
import ru.mifi.learning.dto.review.CreateReviewRequest;
import ru.mifi.learning.dto.review.ReviewResponse;
import ru.mifi.learning.dto.review.UpdateReviewRequest;
import ru.mifi.learning.service.ReviewService;

@RestController
@RequestMapping("/api")
@Tag(name = "Отзывы")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService service;

    @PostMapping("/courses/{id}/reviews")
    @Operation(summary = "Оставить отзыв")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ReviewResponse> create(
            @PathVariable @Positive(message = "Значение должно быть положительным") long id,
            @Valid @RequestBody CreateReviewRequest request) {
        var result = service.create(id, request);
        return ResponseEntity.created(URI.create("/api/reviews/" + result.id())).body(result);
    }

    @GetMapping("/courses/{id}/reviews")
    @Operation(summary = "Отзывы курса")
    public PageResponse<ReviewResponse> list(
            @PathVariable @Positive(message = "Значение должно быть положительным") long id,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return service.list(id, pageable);
    }

    @GetMapping("/reviews/{id}")
    @Operation(summary = "Получить отзыв")
    public ReviewResponse get(@PathVariable @Positive(message = "Значение должно быть положительным") long id) {
        return service.get(id);
    }

    @PutMapping("/reviews/{id}")
    @Operation(summary = "Обновить отзыв")
    public ReviewResponse update(
            @PathVariable @Positive(message = "Значение должно быть положительным") long id,
            @Valid @RequestBody UpdateReviewRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/reviews/{id}")
    @Operation(summary = "Удалить отзыв")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Positive(message = "Значение должно быть положительным") long id) {
        service.delete(id);
    }
}
