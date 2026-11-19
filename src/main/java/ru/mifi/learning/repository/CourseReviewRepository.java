package ru.mifi.learning.repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.mifi.learning.domain.entity.CourseReview;

public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {
    boolean existsByCourseIdAndStudentId(Long courseId, Long studentId);

    @Query(
            value = "select r from CourseReview r join fetch r.student where r.course.id = :id",
            countQuery = "select count(r) from CourseReview r where r.course.id = :id")
    Page<CourseReview> findByCourseIdWithStudent(Long id, Pageable pageable);

    @Query("select avg(r.rating) from CourseReview r where r.course.id = :courseId")
    Optional<Double> averageRatingByCourseId(Long courseId);

    boolean existsByCourseId(Long courseId);

    boolean existsByStudentId(Long studentId);
}
