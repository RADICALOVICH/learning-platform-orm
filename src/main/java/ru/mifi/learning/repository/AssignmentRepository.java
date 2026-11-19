package ru.mifi.learning.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.mifi.learning.domain.entity.Assignment;
import ru.mifi.learning.repository.projection.StudentAssignment;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByLessonId(Long lessonId);

    @Query("select a.lesson.module.course.id from Assignment a where a.id = :id")
    Optional<Long> findCourseIdById(Long id);

    @Query("""
            select new ru.mifi.learning.repository.projection.StudentAssignment(
                a.id, a.title, c.id, c.title, a.dueDate, s.id, s.status, s.score)
            from Assignment a join a.lesson.module.course c
            join Enrollment e on e.course = c and e.student.id = :studentId
            left join Submission s on s.assignment = a and s.student = e.student
            order by c.id, a.id
            """)
    List<StudentAssignment> findStudentAssignments(Long studentId);
}
