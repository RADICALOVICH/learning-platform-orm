package ru.mifi.learning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.mifi.learning.domain.enums.QuestionType;
import ru.mifi.learning.dto.quiz.CreateQuizRequest;
import ru.mifi.learning.dto.quiz.OptionRequest;
import ru.mifi.learning.dto.quiz.QuestionRequest;
import ru.mifi.learning.dto.quiz.TakeQuizRequest;
import ru.mifi.learning.dto.quiz.UpdateQuizRequest;
import ru.mifi.learning.exception.BusinessRuleException;
import ru.mifi.learning.exception.ConflictException;
import ru.mifi.learning.support.AbstractIntegrationTest;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
class QuizServiceIT extends AbstractIntegrationTest {
    private final QuizService quizzes;
    private final QuizSubmissionService attempts;

    private QuestionRequest question() {
        return new QuestionRequest(
                "Выбор",
                QuestionType.SINGLE_CHOICE,
                null,
                List.of(new OptionRequest("Да", true), new OptionRequest("Нет", false)));
    }

    @Test
    @DisplayName("Создание теста каскадом; у студента нет ключа правильных ответов")
    void views() throws Exception {
        var c = data.publishedCourseWithLessons(1);
        var module = c.getModules().getFirst();
        var lesson = module.getLessons().getFirst();
        var input = new CreateQuizRequest("Тест", null, null, null, null, List.of(question()));
        var q = quizzes.create(module.getId(), input);
        assertThat(q.questions().getFirst().options().getFirst().correct()).isTrue();
        queries.reset();
        var student = quizzes.student(q.id());
        queries.assertCount(2);
        assertThat(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(student))
                .doesNotContain("correct");
        assertThatThrownBy(() -> quizzes.create(module.getId(), input)).isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("После попытки доступны метаданные, структура защищена")
    void lock() throws Exception {
        var c = data.publishedCourseWithLessons(1);
        var module = c.getModules().getFirst();
        var lesson = module.getLessons().getFirst();
        var e = data.enrollment(c);
        long student = e.getStudent().getId();
        var q = quizzes.create(
                module.getId(), new CreateQuizRequest("Тест", null, null, null, null, List.of(question())));
        var invalid = new QuestionRequest(
                "Вопрос",
                QuestionType.SINGLE_CHOICE,
                1,
                List.of(new OptionRequest("1", true), new OptionRequest("2", true)));
        assertThatThrownBy(() -> quizzes.add(q.id(), invalid)).isInstanceOf(BusinessRuleException.class);
        attempts.submit(q.id(), new TakeQuizRequest(student, List.of()));
        assertThatThrownBy(() -> quizzes.deleteQuestion(q.questions().getFirst().id()))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> quizzes.add(q.id(), question())).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> quizzes.delete(q.id())).isInstanceOf(ConflictException.class);
        assertThat(quizzes.update(q.id(), new UpdateQuizRequest("Новое", null, null, 80, 2))
                        .title())
                .isEqualTo("Новое");
    }
}
