package ru.mifi.learning.service.quiz;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ru.mifi.learning.exception.BusinessRuleException;

public final class QuizScorer {
    private QuizScorer() {}

    public record Item(long questionId, int points, Set<Long> correctOptions) {}

    public record Mark(long questionId, boolean correct, int points) {}

    public record Score(int earned, int maximum, int percent, boolean passed, List<Mark> marks) {}

    public static Score score(List<Item> questions, Map<Long, Set<Long>> selections, int passingPercent) {
        if (questions.isEmpty()) {
            throw new BusinessRuleException("QUIZ_EMPTY", "Добавьте вопросы перед прохождением теста");
        }
        long maximum = 0;
        int earned = 0;
        List<Mark> marks = new ArrayList<>();
        for (var question : questions) {
            maximum += question.points();
            if (maximum > Integer.MAX_VALUE) {
                throw new BusinessRuleException("INVALID_QUESTION", "Суммарный балл теста слишком велик");
            }
            boolean correct =
                    question.correctOptions().equals(selections.getOrDefault(question.questionId(), Set.of()));
            int points = correct ? question.points() : 0;
            earned += points;
            marks.add(new Mark(question.questionId(), correct, points));
        }
        int percent = (int) Math.round(100.0 * earned / maximum);
        return new Score(earned, (int) maximum, percent, percent >= passingPercent, List.copyOf(marks));
    }
}
