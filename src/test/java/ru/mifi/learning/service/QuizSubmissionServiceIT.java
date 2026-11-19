package ru.mifi.learning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import ru.mifi.learning.domain.entity.AnswerOption;
import ru.mifi.learning.dto.quiz.QuizAnswerRequest;
import ru.mifi.learning.dto.quiz.TakeQuizRequest;
import ru.mifi.learning.dto.quiz.UpdateQuizRequest;
import ru.mifi.learning.exception.BusinessRuleException;
import ru.mifi.learning.exception.ConflictException;
import ru.mifi.learning.service.event.QuizPassedEvent;
import ru.mifi.learning.support.AbstractIntegrationTest;

@RecordApplicationEvents
class QuizSubmissionServiceIT extends AbstractIntegrationTest {
    @Autowired
    QuizService quizzes;

    @Autowired
    QuizSubmissionService attempts;

    @Autowired
    ApplicationEvents events;

    @Test
    @DisplayName("Попытка хранит выбранные варианты и публикует событие только при успехе")
    void result() {
        var c = data.publishedCourseWithLessons(1);
        var module = c.getModules().getFirst();
        var lesson = module.getLessons().getFirst();
        var e = data.enrollment(c);
        long student = e.getStudent().getId();
        var quiz = data.quizWithQuestions(module, 1);
        var q = quiz.getQuestions().getFirst();
        quizzes.update(quiz.getId(), new UpdateQuizRequest("Тест", null, null, 60, 2));
        var fail = attempts.submit(quiz.getId(), new TakeQuizRequest(student, List.of()));
        assertThat(fail.passed()).isFalse();
        assertThat(events.stream(QuizPassedEvent.class)).isEmpty();
        var right = new TakeQuizRequest(
                student, List.of(new QuizAnswerRequest(q.getId(), List.copyOf(q.correctOptionIds()))));
        var result = attempts.submit(quiz.getId(), right);
        assertThat(result.attemptNumber()).isEqualTo(2);
        assertThat(result.percent()).isEqualTo(100);
        assertThat(events.stream(QuizPassedEvent.class)).hasSize(1);
        assertThat(attempts.get(result.id()).answers()).isEqualTo(result.answers());
        assertThat(attempts.list(quiz.getId())).hasSize(2);
        assertThat(attempts.studentResults(student)).hasSize(2);
        assertThatThrownBy(() -> attempts.submit(quiz.getId(), right)).isInstanceOf(ConflictException.class);
        attempts.delete(fail.id());
        assertThat(attempts.submit(quiz.getId(), right).attemptNumber()).isEqualTo(3);
    }

    @Test
    @DisplayName("Посторонние вопросы, варианты и дубликаты отклоняются без расхода попытки")
    void invalid() {
        var c = data.publishedCourseWithLessons(1);
        var module = c.getModules().getFirst();
        var lesson = module.getLessons().getFirst();
        var e = data.enrollment(c);
        long student = e.getStudent().getId();
        var quiz = data.quizWithQuestions(module, 2);
        var a = quiz.getQuestions().getFirst();
        var b = quiz.getQuestions().getLast();
        var foreign = data.quizWithQuestions(
                        data.publishedCourseWithLessons(1).getModules().getFirst(), 1)
                .getQuestions()
                .getFirst();
        var answer = new QuizAnswerRequest(
                a.getId(), List.of(a.getOptions().getFirst().getId()));
        for (var invalid : List.of(
                List.of(answer, answer),
                List.of(new QuizAnswerRequest(foreign.getId(), List.of())),
                List.of(new QuizAnswerRequest(
                        a.getId(), List.of(b.getOptions().getFirst().getId()))),
                List.of(new QuizAnswerRequest(
                        a.getId(),
                        a.getOptions().stream().map(AnswerOption::getId).toList())))) {
            assertThatThrownBy(() -> attempts.submit(quiz.getId(), new TakeQuizRequest(student, invalid)))
                    .isInstanceOf(BusinessRuleException.class)
                    .extracting("code")
                    .isEqualTo("INVALID_ANSWERS");
        }
        assertThatThrownBy(() -> attempts.submit(
                        quiz.getId(), new TakeQuizRequest(data.student().getId(), List.of())))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code")
                .isEqualTo("NOT_ENROLLED");
        assertThat(attempts.list(quiz.getId())).isEmpty();
    }
}
