package ru.mifi.learning.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.mifi.learning.domain.entity.AnswerOption;

public interface AnswerOptionRepository extends JpaRepository<AnswerOption, Long> {
    List<AnswerOption> findAllByQuestionQuizId(Long quizId);
}
