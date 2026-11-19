package ru.mifi.learning.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.mifi.learning.domain.entity.Quiz;
import ru.mifi.learning.repository.projection.ModuleQuizId;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    Optional<Quiz> findByModuleId(Long moduleId);

    boolean existsByModuleId(Long moduleId);

    @EntityGraph(attributePaths = "questions")
    Optional<Quiz> findWithQuestionsById(Long id);

    @Query("""
            select new ru.mifi.learning.repository.projection.ModuleQuizId(q.module.id, q.id)
            from Quiz q where q.module.course.id = :courseId
            """)
    List<ModuleQuizId> findModuleQuizIdsByCourseId(Long courseId);

    @Query("select q.module.course.id from Quiz q where q.id = :id")
    Optional<Long> findCourseIdById(Long id);

    long countByModuleCourseId(Long courseId);
}
