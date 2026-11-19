package ru.mifi.learning.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mifi.learning.domain.entity.Question;

public interface QuestionRepository extends JpaRepository<Question, Long> {}
