package ru.mifi.learning.service.quiz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.mifi.learning.exception.BusinessRuleException;

class QuizScorerTest {
    private final List<QuizScorer.Item> items =
            List.of(new QuizScorer.Item(1, 1, Set.of(10L)), new QuizScorer.Item(2, 2, Set.of(20L, 21L)));

    @Test
    @DisplayName("Полное совпадение вариантов даёт все баллы")
    void allRight() {
        var score = QuizScorer.score(items, Map.of(1L, Set.of(10L), 2L, Set.of(20L, 21L)), 100);
        assertThat(score.earned()).isEqualTo(3);
        assertThat(score.percent()).isEqualTo(100);
        assertThat(score.passed()).isTrue();
    }

    @Test
    @DisplayName("Пропуски и неверные ответы дают ноль")
    void none() {
        for (var answers : List.of(Map.<Long, Set<Long>>of(), Map.of(1L, Set.of(9L), 2L, Set.of(9L)))) {
            var score = QuizScorer.score(items, answers, 1);
            assertThat(score.earned()).isZero();
            assertThat(score.passed()).isFalse();
        }
    }

    @Test
    @DisplayName("Частичный MULTIPLE не засчитывается; проценты округляются")
    void partial() {
        var score = QuizScorer.score(items, Map.of(1L, Set.of(10L), 2L, Set.of(20L)), 33);
        assertThat(score.earned()).isOne();
        assertThat(score.percent()).isEqualTo(33);
        assertThat(score.passed()).isTrue();
        assertThat(QuizScorer.score(items, Map.of(2L, Set.of(21L, 20L)), 68).percent())
                .isEqualTo(67);
    }

    @Test
    @DisplayName("Тест без вопросов нельзя пройти")
    void empty() {
        assertThatThrownBy(() -> QuizScorer.score(List.of(), Map.of(), 0)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Переполнение суммы весов отклоняется до подсчёта результата")
    void excessiveWeight() {
        var questions =
                List.of(new QuizScorer.Item(1, Integer.MAX_VALUE, Set.of(1L)), new QuizScorer.Item(2, 1, Set.of(2L)));
        assertThatThrownBy(() -> QuizScorer.score(questions, Map.of(), 60)).isInstanceOf(BusinessRuleException.class);
    }
}
