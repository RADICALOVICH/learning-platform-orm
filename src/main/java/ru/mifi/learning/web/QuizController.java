package ru.mifi.learning.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
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
import ru.mifi.learning.dto.quiz.CreateQuizRequest;
import ru.mifi.learning.dto.quiz.QuestionRequest;
import ru.mifi.learning.dto.quiz.StudentQuizView;
import ru.mifi.learning.dto.quiz.TeacherQuizView;
import ru.mifi.learning.dto.quiz.UpdateQuizRequest;
import ru.mifi.learning.service.QuizService;

@RestController
@RequestMapping("/api")
@Tag(name = "Учебные тесты")
@RequiredArgsConstructor
public class QuizController {
    private final QuizService service;

    @PostMapping("/modules/{id}/quiz")
    @Operation(summary = "Создать тест")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<TeacherQuizView> create(
            @PathVariable @Positive(message = "Значение должно быть положительным") long id,
            @Valid @RequestBody CreateQuizRequest request) {
        var result = service.create(id, request);
        return ResponseEntity.created(URI.create("/api/quizzes/" + result.id())).body(result);
    }

    @GetMapping("/modules/{id}/quiz")
    @Operation(summary = "Тест модуля")
    public StudentQuizView moduleQuiz(@PathVariable @Positive(message = "Значение должно быть положительным") long id) {
        return service.moduleQuiz(id);
    }

    @GetMapping("/quizzes/{id}")
    @Operation(summary = "Тест для студента")
    public StudentQuizView student(@PathVariable @Positive(message = "Значение должно быть положительным") long id) {
        return service.student(id);
    }

    @GetMapping("/quizzes/{id}/full")
    @Operation(summary = "Тест с ключом ответов")
    public TeacherQuizView teacher(@PathVariable @Positive(message = "Значение должно быть положительным") long id) {
        return service.teacher(id);
    }

    @PutMapping("/quizzes/{id}")
    @Operation(summary = "Обновить параметры теста")
    public TeacherQuizView update(
            @PathVariable @Positive(message = "Значение должно быть положительным") long id,
            @Valid @RequestBody UpdateQuizRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/quizzes/{id}")
    @Operation(summary = "Удалить тест")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Positive(message = "Значение должно быть положительным") long id) {
        service.delete(id);
    }

    @PostMapping("/quizzes/{id}/questions")
    @Operation(summary = "Добавить вопрос")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<TeacherQuizView> add(
            @PathVariable @Positive(message = "Значение должно быть положительным") long id,
            @Valid @RequestBody QuestionRequest request) {
        var q = service.add(id, request);
        return ResponseEntity.created(
                        URI.create("/api/questions/" + q.questions().getLast().id()))
                .body(q);
    }

    @PutMapping("/questions/{id}")
    @Operation(summary = "Изменить вопрос")
    public TeacherQuizView changeQuestion(
            @PathVariable @Positive(message = "Значение должно быть положительным") long id,
            @Valid @RequestBody QuestionRequest request) {
        return service.changeQuestion(id, request);
    }

    @DeleteMapping("/questions/{id}")
    @Operation(summary = "Удалить вопрос")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQuestion(@PathVariable @Positive(message = "Значение должно быть положительным") long id) {
        service.deleteQuestion(id);
    }
}
