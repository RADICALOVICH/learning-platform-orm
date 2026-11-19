package ru.mifi.learning.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import ru.mifi.learning.domain.entity.Course;
import ru.mifi.learning.repository.projection.CourseCardRow;

public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {
    @EntityGraph(attributePaths = {"teacher", "category", "tags"})
    Optional<Course> findDetailsById(Long id);

    @Query("select c from Course c left join fetch c.modules where c.id = :id")
    Optional<Course> findWithModulesById(Long id);

    @Override
    @EntityGraph(attributePaths = {"teacher", "category"})
    Page<Course> findAll(Specification<Course> specification, Pageable pageable);

    boolean existsByCategoryId(Long categoryId);

    boolean existsByTeacherId(Long teacherId);

    @Query("select count(l) from Lesson l where l.module.course.id = :courseId")
    long countLessonsByCourseId(Long courseId);

    @Query("""
        select new ru.mifi.learning.repository.projection.CourseCardRow(c, t.name,
            (select count(m) from CourseModule m where m.course=c),
            (select count(e) from Enrollment e where e.course=c),
            (select avg(r.rating) from CourseReview r where r.course=c))
        from Course c join fetch c.teacher join fetch c.category left join c.tags t where c.id=:id order by t.name
        """)
    List<CourseCardRow> findCardRows(long id);

    @EntityGraph(attributePaths = {"teacher", "category", "tags"})
    List<Course> findByTeacherIdOrderById(Long teacherId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Course c where c.id=:id")
    Optional<Course> lockById(long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Course c where c.id = (select item.module.course.id from Lesson item where item.id = :id)")
    Optional<Course> lockByLessonId(long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Course c where c.id = (select item.course.id from CourseModule item where item.id = :id)")
    Optional<Course> lockByModuleId(long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Course c where c.id = (select item.module.course.id from Quiz item where item.id = :id)")
    Optional<Course> lockByQuizId(long id);
}
