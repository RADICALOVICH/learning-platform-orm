package ru.mifi.learning.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.mifi.learning.domain.entity.QuizSubmission;

public interface QuizSubmissionRepository extends JpaRepository<QuizSubmission, Long> {
    long countByQuizIdAndStudentId(Long quizId, Long studentId);

    boolean existsByQuizId(Long quizId);

    boolean existsByQuizModuleId(Long moduleId);

    boolean existsByQuizModuleCourseId(Long courseId);

    boolean existsByStudentId(Long studentId);

    @Query("select s from QuizSubmission s join fetch s.student where s.quiz.id = :id order by s.id")
    List<QuizSubmission> findByQuizIdWithStudent(Long id);

    @Query("select s from QuizSubmission s join fetch s.quiz where s.student.id = :id order by s.id")
    List<QuizSubmission> findByStudentIdWithQuiz(Long id);

    @Query("""
            select count(distinct s.quiz.id) from QuizSubmission s where s.student.id = :studentId
            and s.quiz.module.course.id = :courseId and s.passed = true
            """)
    long countPassedQuizzes(Long studentId, Long courseId);

    @Query(
            "select coalesce(max(s.attemptNumber),0) from QuizSubmission s where s.quiz.id=:quizId and s.student.id=:studentId")
    int lastAttemptNumber(long quizId, long studentId);

    @EntityGraph(attributePaths = {"quiz.module.course"})
    List<QuizSubmission> findByStudentIdOrderById(long studentId);

    @EntityGraph(attributePaths = "answers")
    Optional<QuizSubmission> findResultById(long id);

    @Query("select s.quiz.id from QuizSubmission s where s.id=:id")
    Optional<Long> findQuizId(long id);
}
