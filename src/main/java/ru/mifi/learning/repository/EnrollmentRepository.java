package ru.mifi.learning.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.mifi.learning.domain.entity.Enrollment;
import ru.mifi.learning.domain.enums.EnrollmentStatus;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

    Optional<Enrollment> findByStudentIdAndCourseId(Long studentId, Long courseId);

    @Query(
            value = "select e from Enrollment e join fetch e.student where e.course.id = :courseId",
            countQuery = "select count(e) from Enrollment e where e.course.id = :courseId")
    Page<Enrollment> findByCourseIdWithStudent(Long courseId, Pageable pageable);

    @Query("""
            select e from Enrollment e join fetch e.course c join fetch c.teacher join fetch c.category
            where e.student.id = :studentId order by e.id
            """)
    List<Enrollment> findByStudentIdWithCourse(Long studentId);

    @Query("select e.student.id from Enrollment e where e.course.id = :courseId and e.status = :status")
    List<Long> findStudentIdsByCourseIdAndStatus(Long courseId, EnrollmentStatus status);

    boolean existsByCourseId(Long courseId);

    boolean existsByStudentId(Long studentId);
}
