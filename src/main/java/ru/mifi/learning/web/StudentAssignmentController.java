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
import ru.mifi.learning.repository.projection.StudentAssignment;
import ru.mifi.learning.service.AssignmentService;

@RestController
@RequestMapping("/api")
@Tag(name = "Задания студента")
@RequiredArgsConstructor
public class StudentAssignmentController {
    private final AssignmentService service;

    @GetMapping("/users/{id}/assignments")
    @Operation(summary = "Задания студента с результатами")
    public List<StudentAssignment> studentAssignments(
            @PathVariable @Positive(message = "Значение должно быть положительным") long id) {
        return service.studentAssignments(id);
    }
}
