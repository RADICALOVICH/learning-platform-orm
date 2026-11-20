package ru.mifi.learning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import ru.mifi.learning.domain.entity.AnswerOption;
import ru.mifi.learning.domain.enums.EnrollmentStatus;
import ru.mifi.learning.domain.enums.QuestionType;
import ru.mifi.learning.dto.enrollment.StudentRequest;
import ru.mifi.learning.dto.quiz.CreateQuizRequest;
import ru.mifi.learning.dto.quiz.OptionRequest;
import ru.mifi.learning.dto.quiz.QuestionRequest;
import ru.mifi.learning.dto.quiz.QuizAnswerRequest;
import ru.mifi.learning.dto.quiz.TakeQuizRequest;
import ru.mifi.learning.dto.quiz.UpdateQuizRequest;
import ru.mifi.learning.exception.BusinessRuleException;
import ru.mifi.learning.exception.ConflictException;
import ru.mifi.learning.service.event.QuizPassedEvent;
import ru.mifi.learning.support.AbstractIntegrationTest;

@RecordApplicationEvents
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class QuizSubmissionServiceIT extends AbstractIntegrationTest {
    private final QuizService quizzes;
    private final QuizSubmissionService attempts;

    @Test
    @DisplayName("Попытка хранит выбранные варианты и публикует событие только при успехе")
    void result(@Autowired ApplicationEvents events) {
        var c = data.publishedCourseWithLessons(1);
        var module = c.getModules().getFirst();
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
    @DisplayName("При пороге 100% результат 199 из 200 оставляет курс активным до полной сдачи")
    void completionUsesExactPassingScore(@Autowired ProgressService progress) {
        var course = data.publishedCourseWithLessons(1);
        var module = course.getModules().getFirst();
        long student = data.enrollment(course).getStudent().getId();
        var options = List.of(new OptionRequest("Да", true), new OptionRequest("Нет", false));
        var questions = List.of(
                new QuestionRequest("Первый", QuestionType.SINGLE_CHOICE, 199, options),
                new QuestionRequest("Второй", QuestionType.SINGLE_CHOICE, 1, options));
        var quiz = quizzes.create(module.getId(), new CreateQuizRequest("Тест", null, null, 100, null, questions));
        progress.mark(module.getLessons().getFirst().getId(), new StudentRequest(student));
        var answers = quiz.questions().stream()
                .map(q -> new QuizAnswerRequest(
                        q.id(), List.of(q.options().getFirst().id())))
                .toList();
        var partial = attempts.submit(quiz.id(), new TakeQuizRequest(student, List.of(answers.getFirst())));
        assertThat(partial.score()).isEqualTo(199);
        assertThat(partial.percent()).isEqualTo(100);
        assertThat(attempts.get(partial.id()).passed()).isFalse();
        assertThat(progress.get(course.getId(), student).status()).isEqualTo(EnrollmentStatus.ACTIVE);

        assertThat(attempts.submit(quiz.id(), new TakeQuizRequest(student, answers))
                        .passed())
                .isTrue();
        assertThat(progress.get(course.getId(), student).status()).isEqualTo(EnrollmentStatus.COMPLETED);
    }

    @Test
    @DisplayName("Посторонние вопросы, варианты и дубликаты отклоняются без расхода попытки")
    void invalid() {
        var c = data.publishedCourseWithLessons(1);
        var module = c.getModules().getFirst();
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
