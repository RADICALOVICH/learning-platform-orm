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
import ru.mifi.learning.dto.quiz.QuizResultResponse;
import ru.mifi.learning.dto.quiz.QuizSubmissionSummary;
import ru.mifi.learning.dto.quiz.TakeQuizRequest;
import ru.mifi.learning.service.QuizSubmissionService;

@RestController
@RequestMapping("/api")
@Tag(name = "Попытки тестирования")
@RequiredArgsConstructor
public class QuizSubmissionController {
    private final QuizSubmissionService service;

    @PostMapping("/quizzes/{id}/submissions")
    @Operation(summary = "Пройти тест")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<QuizResultResponse> submit(
            @PathVariable @Positive(message = "Значение должно быть положительным") long id,
            @Valid @RequestBody TakeQuizRequest request) {
        var result = service.submit(id, request);
        return ResponseEntity.created(URI.create("/api/quiz-submissions/" + result.id()))
                .body(result);
    }

    @GetMapping("/quizzes/{id}/submissions")
    @Operation(summary = "Попытки по тесту")
    public List<QuizSubmissionSummary> list(
            @PathVariable @Positive(message = "Значение должно быть положительным") long id) {
        return service.list(id);
    }

    @GetMapping("/quiz-submissions/{id}")
    @Operation(summary = "Результат с выбранными вариантами")
    public QuizResultResponse get(@PathVariable @Positive(message = "Значение должно быть положительным") long id) {
        return service.get(id);
    }

    @DeleteMapping("/quiz-submissions/{id}")
    @Operation(summary = "Удалить попытку")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Positive(message = "Значение должно быть положительным") long id) {
        service.delete(id);
    }
}
