package ru.mifi.learning.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.mifi.learning.domain.entity.QuizAnswer;

public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Long> {
    @Query("select a from QuizAnswer a left join fetch a.selectedOptions where a.quizSubmission.id=:id")
    List<QuizAnswer> findWithSelectedOptions(long id);
}
