package ru.mifi.learning.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LessonProgressId implements Serializable {
    @Column(name = "enrollment_id")
    private Long enrollmentId;

    @Column(name = "lesson_id")
    private Long lessonId;

    public LessonProgressId(Long enrollmentId, Long lessonId) {
        this.enrollmentId = Objects.requireNonNull(enrollmentId);
        this.lessonId = Objects.requireNonNull(lessonId);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof LessonProgressId key
                && Objects.equals(enrollmentId, key.enrollmentId)
                && Objects.equals(lessonId, key.lessonId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enrollmentId, lessonId);
    }

    @Override
    public String toString() {
        return enrollmentId + "/" + lessonId;
    }
}
