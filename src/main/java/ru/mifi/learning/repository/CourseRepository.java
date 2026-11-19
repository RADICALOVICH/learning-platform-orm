package ru.mifi.learning.repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import ru.mifi.learning.domain.entity.Course;

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
}
