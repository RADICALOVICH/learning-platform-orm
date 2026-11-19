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
import ru.mifi.learning.domain.enums.CourseStatus;
import ru.mifi.learning.dto.common.PageResponse;
import ru.mifi.learning.dto.course.CourseDetailsResponse;
import ru.mifi.learning.dto.course.CourseStructureResponse;
import ru.mifi.learning.dto.course.CourseSummaryResponse;
import ru.mifi.learning.dto.course.CourseTagsRequest;
import ru.mifi.learning.dto.course.CreateCourseRequest;
import ru.mifi.learning.dto.course.UpdateCourseRequest;
import ru.mifi.learning.service.CourseService;

@RestController
@RequestMapping("/api")
@Tag(name = "Курсы")
@RequiredArgsConstructor
public class CourseController {
    private final CourseService service;

    @PostMapping("/courses")
    @Operation(summary = "Создать курс")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<CourseDetailsResponse> create(@Valid @RequestBody CreateCourseRequest request) {
        var result = service.create(request);
        return ResponseEntity.created(URI.create("/api/courses/" + result.id())).body(result);
    }

    @GetMapping("/courses")
    @Operation(summary = "Найти курсы")
    public PageResponse<CourseSummaryResponse> list(
            @RequestParam(required = false) @Positive(message = "Значение должно быть положительным") Long categoryId,
            @RequestParam(required = false) @Positive(message = "Значение должно быть положительным") Long teacherId,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) CourseStatus status,
            @RequestParam(required = false) String q,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return service.list(categoryId, teacherId, tag, status, q, pageable);
    }

    @GetMapping("/courses/{id}")
    @Operation(summary = "Карточка курса")
    public CourseDetailsResponse get(@PathVariable @Positive(message = "Значение должно быть положительным") long id) {
        return service.get(id);
    }

    @GetMapping("/courses/{id}/structure")
    @Operation(summary = "Структура курса")
    public CourseStructureResponse structure(
            @PathVariable @Positive(message = "Значение должно быть положительным") long id) {
        return service.structure(id);
    }

    @PutMapping("/courses/{id}")
    @Operation(summary = "Обновить курс")
    public CourseDetailsResponse update(
            @PathVariable @Positive(message = "Значение должно быть положительным") long id,
            @Valid @RequestBody UpdateCourseRequest request) {
        return service.update(id, request);
    }

    @PutMapping("/courses/{id}/tags")
    @Operation(summary = "Заменить теги")
    public CourseDetailsResponse tags(
            @PathVariable @Positive(message = "Значение должно быть положительным") long id,
            @Valid @RequestBody CourseTagsRequest request) {
        return service.tags(id, request);
    }

    @PostMapping("/courses/{id}/publish")
    @Operation(summary = "Опубликовать курс")
    public CourseDetailsResponse publish(
            @PathVariable @Positive(message = "Значение должно быть положительным") long id) {
        return service.publish(id);
    }

    @PostMapping("/courses/{id}/archive")
    @Operation(summary = "Архивировать курс")
    public CourseDetailsResponse archive(
            @PathVariable @Positive(message = "Значение должно быть положительным") long id) {
        return service.archive(id);
    }

    @DeleteMapping("/courses/{id}")
    @Operation(summary = "Удалить курс")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Positive(message = "Значение должно быть положительным") long id) {
        service.delete(id);
    }
}
