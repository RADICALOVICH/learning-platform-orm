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
    @DisplayName("Повторное сохранение вопроса сохраняет ID вариантов и порядок одинаковых ответов")
    void replacingOptionsPreservesIdsAndOrder() {
        var module = data.publishedCourseWithLessons(1).getModules().getFirst();
        var yes = new OptionRequest("Да", true);
        var no = new OptionRequest("Нет", false);
        var other = new OptionRequest("Другое", false);
        var initial = new QuestionRequest(
                "Выбор", QuestionType.SINGLE_CHOICE, 1, List.of(yes, no, no, new OptionRequest("Пропустить", false)));
        var quiz =
                quizzes.create(module.getId(), new CreateQuizRequest("Тест", null, null, null, null, List.of(initial)));
        var question = quiz.questions().getFirst();
        var old = question.options();
        assertThat(quizzes.changeQuestion(question.id(), initial)).isEqualTo(quiz);

        var edit = new QuestionRequest("Изменённый вопрос", QuestionType.SINGLE_CHOICE, 2, List.of(no, other, no, yes));
        var updated = quizzes.changeQuestion(question.id(), edit);
        var options = updated.questions().getFirst().options();
        assertThat(options.get(0)).isEqualTo(old.get(1));
        assertThat(options.get(2)).isEqualTo(old.get(2));
        assertThat(options.get(3)).isEqualTo(old.get(0));
        assertThat(options.get(1).text()).isEqualTo(other.text());
        assertThat(options.get(1).id()).isNotNull().isNotEqualTo(old.get(3).id());
        assertThat(quizzes.teacher(quiz.id())).isEqualTo(updated);
        assertThat(quizzes.changeQuestion(question.id(), edit)).isEqualTo(updated);
    }

    @Test
    @DisplayName("Поиск теста различает неизвестный модуль и существующий модуль без теста")
    void missingModuleQuiz() {
        var module = data.publishedCourseWithLessons(1).getModules().getFirst();
        assertThatThrownBy(() -> quizzes.moduleQuiz(module.getId()))
                .hasMessage("В модуле с идентификатором %d нет теста", module.getId())
                .extracting("code")
                .isEqualTo("QUIZ_NOT_FOUND");
        assertThatThrownBy(() -> quizzes.moduleQuiz(Long.MAX_VALUE))
                .extracting("code")
                .isEqualTo("MODULE_NOT_FOUND");
    }

    @Test
    @DisplayName("Тест принимает предельную сумму баллов и отклоняет её превышение при создании и правке")
    void totalPointsLimit() {
        var module = data.publishedCourseWithLessons(1).getModules().getFirst();
        var one = question();
        var high = new QuestionRequest(one.text(), one.type(), Integer.MAX_VALUE - 1, one.options());
        var two = new QuestionRequest(one.text(), one.type(), 2, one.options());
        assertThatThrownBy(() -> quizzes.create(
                        module.getId(), new CreateQuizRequest("Тест", null, null, null, null, List.of(high, two))))
                .extracting("code")
                .isEqualTo("INVALID_QUESTION");
        var quiz = quizzes.create(
                module.getId(), new CreateQuizRequest("Тест", null, null, null, null, List.of(high, one)));
        assertThatThrownBy(() -> quizzes.add(quiz.id(), one)).extracting("code").isEqualTo("INVALID_QUESTION");
        assertThatThrownBy(
                        () -> quizzes.changeQuestion(quiz.questions().getLast().id(), two))
                .extracting("code")
                .isEqualTo("INVALID_QUESTION");
        assertThat(quizzes.teacher(quiz.id())).isEqualTo(quiz);
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
