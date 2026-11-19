package ru.mifi.learning.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mifi.learning.domain.entity.LessonProgress;
import ru.mifi.learning.domain.entity.LessonProgressId;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, LessonProgressId> {
    long countByEnrollmentId(Long enrollmentId);
}
