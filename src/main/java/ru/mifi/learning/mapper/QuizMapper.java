package ru.mifi.learning.mapper;

import java.util.Comparator;
import org.springframework.stereotype.Component;
import ru.mifi.learning.domain.entity.AnswerOption;
import ru.mifi.learning.domain.entity.Question;
import ru.mifi.learning.domain.entity.Quiz;
import ru.mifi.learning.domain.entity.QuizSubmission;
import ru.mifi.learning.dto.quiz.AnswerResult;
import ru.mifi.learning.dto.quiz.QuizResultResponse;
import ru.mifi.learning.dto.quiz.QuizSubmissionSummary;
import ru.mifi.learning.dto.quiz.StudentOptionView;
import ru.mifi.learning.dto.quiz.StudentQuestionView;
import ru.mifi.learning.dto.quiz.StudentQuizResultResponse;
import ru.mifi.learning.dto.quiz.StudentQuizView;
import ru.mifi.learning.dto.quiz.TeacherOptionView;
import ru.mifi.learning.dto.quiz.TeacherQuestionView;
import ru.mifi.learning.dto.quiz.TeacherQuizView;

@Component
public class QuizMapper {
    public TeacherQuizView teacher(Quiz quiz) {
        return new TeacherQuizView(
                quiz.getId(),
                quiz.getModule().getId(),
                quiz.getTitle(),
                quiz.getDescription(),
                quiz.getTimeLimitMinutes(),
                quiz.getPassingScorePercent(),
                quiz.getMaxAttempts(),
                quiz.getQuestions().stream()
                        .map(item -> new TeacherQuestionView(
                                item.getId(),
                                item.getText(),
                                item.getType(),
                                item.getPoints(),
                                item.getOrderIndex(),
                                item.getOptions().stream()
                                        .map(option -> new TeacherOptionView(
                                                option.getId(), option.getText(), option.isCorrect()))
                                        .toList()))
                        .toList());
    }

    public StudentQuizView student(Quiz quiz) {
        return new StudentQuizView(
                quiz.getId(),
                quiz.getModule().getId(),
                quiz.getTitle(),
                quiz.getDescription(),
                quiz.getTimeLimitMinutes(),
                quiz.getPassingScorePercent(),
                quiz.getMaxAttempts(),
                quiz.getQuestions().stream().map(this::studentQuestion).toList());
    }

    public StudentQuestionView studentQuestion(Question item) {
        return new StudentQuestionView(
                item.getId(),
                item.getText(),
                item.getType(),
                item.getPoints(),
                item.getOrderIndex(),
                item.getOptions().stream()
                        .map(option -> new StudentOptionView(option.getId(), option.getText()))
                        .toList());
    }

    public QuizResultResponse result(QuizSubmission submission) {
        return new QuizResultResponse(
                submission.getId(),
                submission.getQuiz().getId(),
                submission.getStudent().getId(),
                submission.getAttemptNumber(),
                submission.getScore(),
                submission.getMaxScore(),
                submission.getScorePercent(),
                submission.isPassed(),
                submission.getTakenAt(),
                submission.getAnswers().stream()
                        .sorted(Comparator.comparing(
                                answer -> answer.getQuestion().getId()))
                        .map(answer -> new AnswerResult(
                                answer.getQuestion().getId(),
                                answer.getSelectedOptions().stream()
                                        .map(AnswerOption::getId)
                                        .sorted()
                                        .toList(),
                                answer.isCorrect(),
                                answer.getPointsAwarded()))
                        .toList());
    }

    public QuizSubmissionSummary summary(QuizSubmission submission) {
        return new QuizSubmissionSummary(
                submission.getId(),
                submission.getStudent().getId(),
                submission.getStudent().getFullName(),
                submission.getAttemptNumber(),
                submission.getScorePercent(),
                submission.isPassed(),
                submission.getTakenAt());
    }

    public StudentQuizResultResponse studentResult(QuizSubmission submission) {
        var quiz = submission.getQuiz();
        var module = quiz.getModule();
        var course = module.getCourse();
        return new StudentQuizResultResponse(
                submission.getId(),
                quiz.getId(),
                quiz.getTitle(),
                module.getId(),
                module.getTitle(),
                course.getId(),
                course.getTitle(),
                submission.getAttemptNumber(),
                submission.getScorePercent(),
                submission.isPassed(),
                submission.getTakenAt());
    }
}
