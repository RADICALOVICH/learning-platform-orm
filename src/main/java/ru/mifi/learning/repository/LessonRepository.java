package ru.mifi.learning.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.mifi.learning.domain.entity.Lesson;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    @Query("select max(l.orderIndex) from Lesson l where l.module.id = :moduleId")
    Optional<Integer> findMaxOrderIndexByModuleId(Long moduleId);

    @Query("select l.module.course.id from Lesson l where l.id = :id")
    Optional<Long> findCourseIdById(Long id);

    long countByModuleCourseId(Long courseId);
}
