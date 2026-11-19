package ru.mifi.learning.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "course_reviews",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_course_reviews_course_student",
                    columnNames = {"course_id", "student_id"})
        },
        indexes = {@Index(name = "idx_course_reviews_student", columnList = "student_id")})
@Check(constraints = "rating between 1 and 5")
public class CourseReview extends BaseEntity {
    @Setter
    @Column(nullable = false)
    private int rating;

    @Setter
    @Column(columnDefinition = "text")
    private String comment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false, foreignKey = @ForeignKey(name = "fk_course_reviews_student"))
    private User student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false, foreignKey = @ForeignKey(name = "fk_course_reviews_course"))
    private Course course;

    public CourseReview(Course course, User student, int rating) {
        this.course = course;
        this.student = student;
        this.rating = rating;
        course.addReview(this);
    }
}
