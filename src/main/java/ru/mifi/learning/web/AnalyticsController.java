package ru.mifi.learning.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.mifi.learning.dto.analytics.CourseAnalytics;
import ru.mifi.learning.dto.analytics.LeaderboardEntry;
import ru.mifi.learning.dto.analytics.QuizAnalytics;
import ru.mifi.learning.dto.analytics.RatedCourse;
import ru.mifi.learning.dto.analytics.StudentCourseAnalytics;
import ru.mifi.learning.service.AnalyticsService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analytics")
@Tag(name = "Аналитика")
public class AnalyticsController {
    private final AnalyticsService service;

    @GetMapping("/students/{id}")
    @Operation(
            summary = "Успеваемость студента по курсам",
            description =
                    "Средний процент за задания учитывает оценённые решения. Лучший процент за тест берётся по всем попыткам курса, а каждый сданный тест учитывается один раз.")
    public List<StudentCourseAnalytics> student(
            @PathVariable @Positive(message = "Значение должно быть положительным") long id) {
        return service.student(id);
    }

    @GetMapping("/courses/{id}")
    @Operation(
            summary = "Статистика курса",
            description =
                    "Доля завершивших считается по записям, а доля успешных попыток — по всем попыткам тестов курса. Средние и доли без наблюдений возвращаются как null.")
    public CourseAnalytics course(@PathVariable @Positive(message = "Значение должно быть положительным") long id) {
        return service.course(id);
    }

    @GetMapping("/courses/{id}/leaderboard")
    @Operation(
            summary = "Рейтинг студентов курса",
            description =
                    "К баллам за задания прибавляется лучшая попытка каждого теста. При равенстве баллов место совпадает. Лимит задаёт число строк.")
    public List<LeaderboardEntry> leaderboard(
            @PathVariable @Positive(message = "Значение должно быть положительным") long id,
            @RequestParam(defaultValue = "10") @Positive(message = "Лимит должен быть положительным") int limit) {
        return service.leaderboard(id, limit);
    }

    @GetMapping("/quizzes/{id}")
    @Operation(
            summary = "Результаты теста и сложность вопросов",
            description =
                    "Самый трудный вопрос имеет наименьший процент правильных ответов. При равенстве выбирается первый в порядке теста.")
    public QuizAnalytics quiz(@PathVariable @Positive(message = "Значение должно быть положительным") long id) {
        return service.quiz(id);
    }

    @GetMapping("/courses/top-rated")
    @Operation(
            summary = "Курсы с наивысшим средним рейтингом",
            description =
                    "При равном рейтинге выше курс с большим числом отзывов. В подборку входят курсы хотя бы с одним отзывом.")
    public List<RatedCourse> topRated(
            @RequestParam(defaultValue = "1")
                    @PositiveOrZero(message = "Минимальное число отзывов должно быть неотрицательным")
                    int minReviews,
            @RequestParam(defaultValue = "5") @Positive(message = "Лимит должен быть положительным") int limit) {
        return service.topRated(minReviews, limit);
    }
}
