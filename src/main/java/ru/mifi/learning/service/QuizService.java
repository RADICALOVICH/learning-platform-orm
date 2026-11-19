package ru.mifi.learning.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mifi.learning.domain.entity.AnswerOption;
import ru.mifi.learning.domain.entity.Question;
import ru.mifi.learning.domain.entity.Quiz;
import ru.mifi.learning.domain.enums.QuestionType;
import ru.mifi.learning.dto.quiz.CreateQuizRequest;
import ru.mifi.learning.dto.quiz.OptionRequest;
import ru.mifi.learning.dto.quiz.QuestionRequest;
import ru.mifi.learning.dto.quiz.StudentQuestionView;
import ru.mifi.learning.dto.quiz.StudentQuizView;
import ru.mifi.learning.dto.quiz.TeacherQuizView;
import ru.mifi.learning.dto.quiz.UpdateQuizRequest;
import ru.mifi.learning.exception.BusinessRuleException;
import ru.mifi.learning.exception.ConflictException;
import ru.mifi.learning.exception.NotFoundException;
import ru.mifi.learning.mapper.QuizMapper;
import ru.mifi.learning.repository.CourseModuleRepository;
import ru.mifi.learning.repository.CourseRepository;
import ru.mifi.learning.repository.QuestionRepository;
import ru.mifi.learning.repository.QuizRepository;
import ru.mifi.learning.repository.QuizSubmissionRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizService {
    private final QuizRepository quizzes;
    private final CourseModuleRepository modules;
    private final CourseRepository courses;
    private final CourseCompletionService completion;
    private final QuestionRepository questions;
    private final QuizSubmissionRepository attempts;
    private final QuizMapper mapper;

    @Transactional
    public TeacherQuizView create(long id, CreateQuizRequest request) {
        var module = modules.lockById(id).orElseThrow(() -> NotFoundException.of("Module", id));
        if (quizzes.existsByModuleId(id)) {
            throw new ConflictException("QUIZ_ALREADY_EXISTS", "В модуле уже есть тест");
        }
        var quiz = new Quiz(module, request.title());
        quiz.setDescription(request.description());
        quiz.setTimeLimitMinutes(request.timeLimitMinutes());
        quiz.setMaxAttempts(request.maxAttempts());
        if (request.passingScorePercent() != null) {
            quiz.setPassingScorePercent(request.passingScorePercent());
        }
        for (var input : request.questions()) {
            quiz.addQuestion(newQuestion(input));
        }
        quiz.totalPoints();
        return mapper.teacher(quizzes.save(quiz));
    }

    public StudentQuizView moduleQuiz(long id) {
        var quiz = quizzes.findWithQuestionsByModuleId(id)
                .orElseThrow(() -> modules.existsById(id)
                        ? NotFoundException.quizForModule(id)
                        : NotFoundException.of("Module", id));
        return mapper.student(quiz);
    }

    public StudentQuizView student(long id) {
        return mapper.student(read(id));
    }

    public TeacherQuizView teacher(long id) {
        return mapper.teacher(read(id));
    }

    @Transactional
    public TeacherQuizView update(long id, UpdateQuizRequest request) {
        var quiz = lock(id);
        quiz.setTitle(request.title());
        quiz.setDescription(request.description());
        quiz.setTimeLimitMinutes(request.timeLimitMinutes());
        quiz.setMaxAttempts(request.maxAttempts());
        quiz.setPassingScorePercent(request.passingScorePercent());
        return mapper.teacher(quiz);
    }

    @Transactional
    public void delete(long id) {
        var course = courses.lockByQuizId(id).orElseThrow(() -> NotFoundException.of("Quiz", id));
        var quiz = editable(id);
        quizzes.delete(quiz);
        completion.afterContentRemoval(course);
    }

    @Transactional
    public TeacherQuizView add(long id, QuestionRequest input) {
        var quiz = editable(id);
        var item = newQuestion(input);
        quiz.addQuestion(item);
        quiz.totalPoints();
        questions.save(item);
        return mapper.teacher(quiz);
    }

    @Transactional
    public TeacherQuizView changeQuestion(long id, QuestionRequest input) {
        var quiz = parentForEdit(id);
        var item = questions.findById(id).orElseThrow(() -> NotFoundException.of("Question", id));
        apply(item, input);
        quiz.totalPoints();
        questions.flush();
        return mapper.teacher(quiz);
    }

    @Transactional
    public void deleteQuestion(long id) {
        var quiz = parentForEdit(id);
        quiz.removeQuestion(questions.getReferenceById(id));
    }

    public StudentQuestionView question(long id) {
        return mapper.studentQuestion(questions.findById(id).orElseThrow(() -> NotFoundException.of("Question", id)));
    }

    private Quiz parentForEdit(long id) {
        return editable(questions.findQuizId(id).orElseThrow(() -> NotFoundException.of("Question", id)));
    }

    private Quiz editable(long id) {
        var quiz = lock(id);
        if (attempts.existsByQuizId(id)) {
            throw new ConflictException(
                    "QUIZ_LOCKED", "После прохождения теста нельзя менять вопросы или удалять тест");
        }
        return quiz;
    }

    private Quiz lock(long id) {
        return quizzes.lockById(id).orElseThrow(() -> NotFoundException.of("Quiz", id));
    }

    private Quiz read(long id) {
        return quizzes.findWithQuestionsById(id).orElseThrow(() -> NotFoundException.of("Quiz", id));
    }

    private static Question newQuestion(QuestionRequest input) {
        var question = new Question(input.text(), input.type());
        apply(question, input);
        return question;
    }

    private static void apply(Question question, QuestionRequest input) {
        long correct = input.options().stream().filter(OptionRequest::correct).count();
        if (input.options().size() < 2
                || correct == 0
                || (input.type() == QuestionType.SINGLE_CHOICE && correct != 1)) {
            throw new BusinessRuleException(
                    "INVALID_QUESTION", "Нужны два варианта и корректный набор правильных ответов");
        }
        question.setText(input.text());
        question.setType(input.type());
        question.setPoints(input.points() == null ? 1 : input.points());
        List.copyOf(question.getOptions()).forEach(question::removeOption);
        for (var option : input.options()) {
            question.addOption(new AnswerOption(option.text(), option.correct()));
        }
    }
}
