package ru.mifi.learning.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.mifi.learning.domain.entity.Submission;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    boolean existsByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    @Query("select s from Submission s join fetch s.student where s.assignment.id = :id order by s.id")
    List<Submission> findByAssignmentIdWithStudent(Long id);

    @Query("""
            select s from Submission s join fetch s.assignment a join fetch a.lesson l
            join fetch l.module m join fetch m.course where s.student.id = :id order by s.id
            """)
    List<Submission> findByStudentIdWithAssignment(Long id);

    boolean existsByAssignmentId(Long id);

    boolean existsByAssignmentIdAndScoreGreaterThan(long assignmentId, int score);

    boolean existsByAssignmentLessonId(Long id);

    boolean existsByAssignmentLessonModuleId(Long id);

    boolean existsByAssignmentLessonModuleCourseId(Long id);

    boolean existsByStudentId(Long id);
}
