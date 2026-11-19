package ru.mifi.learning.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import ru.mifi.learning.domain.entity.Enrollment;
import ru.mifi.learning.domain.enums.EnrollmentStatus;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

    Optional<Enrollment> findByStudentIdAndCourseId(Long studentId, Long courseId);

    @EntityGraph(attributePaths = "student")
    Page<Enrollment> findWithStudentByCourseId(Long courseId, Pageable pageable);

    @Query("""
            select e from Enrollment e join fetch e.course c join fetch c.teacher join fetch c.category
            where e.student.id = :studentId order by e.id
            """)
    List<Enrollment> findByStudentIdWithCourse(Long studentId);

    @Query("select e.student.id from Enrollment e where e.course.id = :courseId and e.status = :status")
    List<Long> findStudentIdsByCourseIdAndStatus(Long courseId, EnrollmentStatus status);

    boolean existsByCourseId(Long courseId);

    boolean existsByStudentId(Long studentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Enrollment e where e.student.id=:studentId and e.course.id=:courseId")
    Optional<Enrollment> lockByStudentAndCourse(long studentId, long courseId);
}
