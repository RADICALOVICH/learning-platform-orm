package ru.mifi.learning.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.mifi.learning.domain.entity.Question;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    @Query("select q.quiz.id from Question q where q.id=:id")
    Optional<Long> findQuizId(long id);
}
