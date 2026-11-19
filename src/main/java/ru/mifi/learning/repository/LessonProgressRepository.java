package ru.mifi.learning.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.mifi.learning.domain.entity.LessonProgress;
import ru.mifi.learning.domain.entity.LessonProgressId;
import ru.mifi.learning.dto.enrollment.LessonProgressResponse;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, LessonProgressId> {
    long countByEnrollmentId(Long enrollmentId);

    @Query("""
            select new ru.mifi.learning.dto.enrollment.LessonProgressResponse(
              p.lesson.id, e.student.id, e.course.id, p.completedAt)
            from LessonProgress p join p.enrollment e
            where p.lesson.id = :lessonId and e.student.id = :studentId
            """)
    Optional<LessonProgressResponse> findCompletion(long lessonId, long studentId);
}
