package ru.mifi.learning.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import ru.mifi.learning.domain.entity.CourseModule;

public interface CourseModuleRepository extends JpaRepository<CourseModule, Long> {
    @Query(
            "select m from CourseModule m left join fetch m.lessons where m.course.id = :courseId order by m.orderIndex, m.id")
    List<CourseModule> findWithLessonsByCourseId(Long courseId);

    @Query("select max(m.orderIndex) from CourseModule m where m.course.id = :courseId")
    Optional<Integer> findMaxOrderIndexByCourseId(Long courseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from CourseModule m where m.id=:id")
    Optional<CourseModule> lockById(long id);
}
