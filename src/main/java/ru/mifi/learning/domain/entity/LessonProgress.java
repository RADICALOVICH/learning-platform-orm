package ru.mifi.learning.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "lesson_progress",
        indexes = {@Index(name = "idx_lesson_progress_lesson", columnList = "lesson_id")})
public class LessonProgress extends AbstractEntity<LessonProgressId> {
    @EmbeddedId
    private LessonProgressId id;

    @Column(nullable = false)
    private Instant completedAt;

    @MapsId("enrollmentId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "enrollment_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_lesson_progress_enrollment"))
    private Enrollment enrollment;

    @MapsId("lessonId")
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false, foreignKey = @ForeignKey(name = "fk_lesson_progress_lesson"))
    private Lesson lesson;

    public LessonProgress(Enrollment enrollment, Lesson lesson, Instant completedAt) {
        this.enrollment = enrollment;
        this.lesson = lesson;
        this.completedAt = completedAt;
        this.id = new LessonProgressId(enrollment.getId(), lesson.getId());
    }
}
