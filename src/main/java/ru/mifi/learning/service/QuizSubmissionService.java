package ru.mifi.learning.service;

import java.time.Clock;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mifi.learning.domain.entity.AnswerOption;
import ru.mifi.learning.domain.entity.Question;
import ru.mifi.learning.domain.entity.QuizAnswer;
import ru.mifi.learning.domain.entity.QuizSubmission;
import ru.mifi.learning.domain.enums.QuestionType;
import ru.mifi.learning.dto.quiz.QuizAnswerRequest;
import ru.mifi.learning.dto.quiz.QuizResultResponse;
import ru.mifi.learning.dto.quiz.QuizSubmissionSummary;
import ru.mifi.learning.dto.quiz.StudentQuizResultResponse;
import ru.mifi.learning.dto.quiz.TakeQuizRequest;
import ru.mifi.learning.exception.BusinessRuleException;
import ru.mifi.learning.exception.ConflictException;
import ru.mifi.learning.exception.NotFoundException;
import ru.mifi.learning.mapper.QuizMapper;
import ru.mifi.learning.repository.EnrollmentRepository;
import ru.mifi.learning.repository.QuizAnswerRepository;
import ru.mifi.learning.repository.QuizRepository;
import ru.mifi.learning.repository.QuizSubmissionRepository;
import ru.mifi.learning.repository.UserRepository;
import ru.mifi.learning.service.event.QuizPassedEvent;
import ru.mifi.learning.service.quiz.QuizScorer;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizSubmissionService {
    private final QuizRepository quizzes;
    private final QuizSubmissionRepository attempts;
    private final QuizAnswerRepository answers;
    private final EnrollmentRepository enrollments;
    private final UserRepository users;
    private final Clock clock;
    private final ApplicationEventPublisher events;
    private final QuizMapper mapper;

    @Transactional
    public QuizResultResponse submit(long id, TakeQuizRequest request) {
        var quiz = quizzes.lockById(id).orElseThrow(() -> NotFoundException.of("Quiz", id));
        long courseId = quiz.getModule().getCourse().getId();
        var enrollment = enrollments
                .lockByStudentAndCourse(request.studentId(), courseId)
                .orElseThrow(
                        () -> new BusinessRuleException("NOT_ENROLLED", "Для прохождения теста нужна запись на курс"));
        if (quiz.getMaxAttempts() != null
                && attempts.countByQuizIdAndStudentId(id, request.studentId()) >= quiz.getMaxAttempts()) {
            throw new ConflictException("ATTEMPTS_EXCEEDED", "Попытки закончились");
        }
        int number = attempts.lastAttemptNumber(id, request.studentId()) + 1;
        quizzes.findWithQuestionsById(id);
        Map<Long, Question> byId =
                quiz.getQuestions().stream().collect(Collectors.toMap(Question::getId, Function.identity()));
        Map<Long, Set<Long>> selections = validate(request.answers(), byId);
        var items = quiz.getQuestions().stream()
                .map(question ->
                        new QuizScorer.Item(question.getId(), question.getPoints(), question.correctOptionIds()))
                .toList();
        var score = QuizScorer.score(items, selections, quiz.getPassingScorePercent());
        var result = new QuizSubmission(
                quiz,
                enrollment.getStudent(),
                number,
                score.earned(),
                score.maximum(),
                score.percent(),
                score.passed(),
                clock.instant());
        for (var mark : score.marks()) {
            var question = byId.get(mark.questionId());
            var chosen = selections.getOrDefault(mark.questionId(), Set.of());
            var options = question.getOptions().stream()
                    .filter(option -> chosen.contains(option.getId()))
                    .toList();
            result.addAnswer(new QuizAnswer(question, mark.correct(), mark.points(), options));
        }
        attempts.save(result);
        log.info("Сохранена попытка id={}, quizId={}", result.getId(), id);
        if (result.isPassed()) {
            events.publishEvent(new QuizPassedEvent(request.studentId(), id, courseId));
        }
        return mapper.result(result);
    }

    private Map<Long, Set<Long>> validate(List<QuizAnswerRequest> input, Map<Long, Question> questions) {
        Map<Long, Set<Long>> selections = new HashMap<>();
        for (var answer : input) {
            var question = questions.get(answer.questionId());
            var ids = new HashSet<>(answer.optionIds());
            if (question == null || selections.containsKey(answer.questionId())) {
                throw new BusinessRuleException("INVALID_ANSWERS", "Вопрос отсутствует в тесте или указан повторно");
            }
            if (question.getType() == QuestionType.SINGLE_CHOICE && ids.size() > 1) {
                throw new BusinessRuleException(
                        "INVALID_ANSWERS", "В вопросе с одиночным выбором укажите один вариант");
            }
            var allowed =
                    question.getOptions().stream().map(AnswerOption::getId).collect(Collectors.toSet());
            if (!allowed.containsAll(ids)) {
                throw new BusinessRuleException("INVALID_ANSWERS", "Выбранный вариант не относится к вопросу");
            }
            selections.put(question.getId(), ids);
        }
        return selections;
    }

    public QuizResultResponse get(long id) {
        var submission = attempts.findResultById(id).orElseThrow(() -> NotFoundException.of("QuizSubmission", id));
        answers.findWithSelectedOptions(id);
        return mapper.result(submission);
    }

    public List<QuizSubmissionSummary> list(long id) {
        var rows = attempts.findByQuizIdWithStudent(id);
        if (rows.isEmpty() && !quizzes.existsById(id)) {
            throw NotFoundException.of("Quiz", id);
        }
        return rows.stream().map(mapper::summary).toList();
    }

    public List<StudentQuizResultResponse> studentResults(long id) {
        var rows = attempts.findByStudentIdOrderById(id);
        if (rows.isEmpty() && !users.existsById(id)) {
            throw NotFoundException.of("User", id);
        }
        return rows.stream().map(mapper::studentResult).toList();
    }

    @Transactional
    public void delete(long id) {
        long quiz = attempts.findQuizId(id).orElseThrow(() -> NotFoundException.of("QuizSubmission", id));
        quizzes.lockById(quiz);
        var attempt = attempts.findById(id).orElseThrow(() -> NotFoundException.of("QuizSubmission", id));
        attempts.delete(attempt);
    }
}
