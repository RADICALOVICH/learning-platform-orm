package ru.mifi.learning.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import ru.mifi.learning.domain.entity.AnswerOption;
import ru.mifi.learning.domain.entity.Course;
import ru.mifi.learning.domain.entity.CourseModule;
import ru.mifi.learning.domain.entity.Question;
import ru.mifi.learning.domain.entity.Quiz;
import ru.mifi.learning.domain.entity.QuizAnswer;
import ru.mifi.learning.domain.entity.QuizSubmission;
import ru.mifi.learning.domain.entity.User;
import ru.mifi.learning.repository.projection.ModuleQuizId;
import ru.mifi.learning.support.AbstractRepositoryTest;
import ru.mifi.learning.support.TestDataFactory;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
class QuizRepositoryIT extends AbstractRepositoryTest {
    private final QuizRepository quizzes;
    private final QuizSubmissionRepository attempts;

    @Test
    @DisplayName("Вопросы и варианты сохраняются каскадом")
    void tree() {
        Course course = data.publishedCourseWithLessons(1);
        CourseModule module = course.getModules().getFirst();
        Quiz quiz = data.quizWithQuestions(module, 2);
        flushAndClear();
        Quiz loaded = quizzes.findByModuleId(module.getId()).orElseThrow();
        assertThat(loaded.totalPoints()).isEqualTo(2);
        assertThat(loaded.getQuestions())
                .allSatisfy(question -> assertThat(question.correctOptionIds()).hasSize(1));
        assertThat(quizzes.findModuleQuizIdsByCourseId(course.getId()))
                .containsExactly(new ModuleQuizId(module.getId(), quiz.getId()));
    }

    @Test
    @DisplayName("У модуля может быть только один тест")
    void unique() {
        Course course = data.publishedCourseWithLessons(1);
        CourseModule module = course.getModules().getFirst();

        data.quizWithQuestions(module, 1);
        em.flush();
        assertThatThrownBy(() -> quizzes.saveAndFlush(new Quiz(module, "Дубль")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Варианты для трёх вопросов подгружаются одной пачкой")
    void batch() {
        Course course = data.publishedCourseWithLessons(1);
        CourseModule module = course.getModules().getFirst();

        Quiz quiz = data.quizWithQuestions(module, 3);
        flushAndClear();
        queries.reset();
        Quiz loaded = quizzes.findWithQuestionsById(quiz.getId()).orElseThrow();
        queries.assertCount(1);
        assertThat(loaded.getQuestions())
                .hasSize(3)
                .allSatisfy(question -> assertThat(question.getOptions()).hasSize(2));
        queries.assertCount(2);
    }

    @Test
    @DisplayName("Попытка сохраняет выбранные варианты и удаляется независимо от теста")
    void selected() {
        Course course = data.publishedCourseWithLessons(1);
        CourseModule module = course.getModules().getFirst();

        Quiz quiz = data.quizWithQuestions(module, 1);
        Question question = quiz.getQuestions().getFirst();
        AnswerOption selected = question.getOptions().getFirst();
        User student = data.student();
        QuizSubmission attempt = new QuizSubmission(quiz, student, 1, 1, 1, 100, true, TestDataFactory.NOW);
        attempt.addAnswer(new QuizAnswer(question, true, 1, List.of(selected)));
        attempts.save(attempt);
        flushAndClear();
        QuizSubmission loaded = attempts.findById(attempt.getId()).orElseThrow();
        assertThat(loaded.getAnswers().getFirst().getSelectedOptions()).containsExactly(selected);
        assertThat(attempts.countPassedQuizzes(student.getId(), course.getId())).isOne();
        attempts.delete(loaded);
        flushAndClear();
        assertThat(em.find(AnswerOption.class, selected.getId())).isNotNull();
    }
}
