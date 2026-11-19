package ru.mifi.learning.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mifi.learning.domain.entity.QuizAnswer;

public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Long> {}
