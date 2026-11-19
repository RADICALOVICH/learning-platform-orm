package ru.mifi.learning.repository;

import java.util.List;
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
}
