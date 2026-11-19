package ru.mifi.learning.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.mifi.learning.dto.quiz.StudentQuizResultResponse;
import ru.mifi.learning.service.QuizSubmissionService;

@RestController
@RequestMapping("/api")
@Tag(name = "Результаты студента")
@RequiredArgsConstructor
public class StudentQuizController {
    private final QuizSubmissionService service;

    @GetMapping("/users/{id}/quiz-submissions")
    @Operation(summary = "Попытки студента")
    public List<StudentQuizResultResponse> studentResults(
            @PathVariable @Positive(message = "Значение должно быть положительным") long id) {
        return service.studentResults(id);
    }
}
