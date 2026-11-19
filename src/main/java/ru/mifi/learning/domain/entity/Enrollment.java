package ru.mifi.learning.domain.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ru.mifi.learning.domain.enums.EnrollmentStatus;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "enrollments",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_enrollments_student_course",
                    columnNames = {"student_id", "course_id"})
        },
        indexes = {@Index(name = "idx_enrollments_course", columnList = "course_id")})
public class Enrollment extends BaseEntity {
    @Column(nullable = false)
    private Instant enrolledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

    private Instant completedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false, foreignKey = @ForeignKey(name = "fk_enrollments_student"))
    private User student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false, foreignKey = @ForeignKey(name = "fk_enrollments_course"))
    private Course course;

    @OneToMany(mappedBy = "enrollment", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LessonProgress> progress = new ArrayList<>();

    public Enrollment(User student, Course course, Instant enrolledAt) {
        this.student = student;
        this.course = course;
        this.enrolledAt = enrolledAt;
        student.addEnrollment(this);
        course.addEnrollment(this);
    }

    public void complete(Instant now) {
        if (status == EnrollmentStatus.ACTIVE) {
            completedAt = now;
            status = EnrollmentStatus.COMPLETED;
        }
    }

    public LessonProgress markLessonCompleted(Lesson lesson, Instant now) {
        LessonProgress entry = new LessonProgress(this, lesson, now);
        int existing = progress.indexOf(entry);
        if (existing >= 0) {
            return progress.get(existing);
        }
        progress.add(entry);
        return entry;
    }

    public List<LessonProgress> getProgress() {
        return Collections.unmodifiableList(progress);
    }
}
