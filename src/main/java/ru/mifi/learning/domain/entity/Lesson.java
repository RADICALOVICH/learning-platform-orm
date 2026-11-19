package ru.mifi.learning.domain.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "lessons",
        indexes = {@Index(name = "idx_lessons_module", columnList = "module_id")})
@Check(constraints = "duration_minutes > 0")
public class Lesson extends BaseEntity {
    @Setter
    @Column(length = 200, nullable = false)
    private String title;

    @Setter
    @Column(columnDefinition = "text")
    private String content;

    @Setter
    @Column(length = 500)
    private String videoUrl;

    @Setter
    @Column(nullable = false)
    private Integer orderIndex;

    @Setter
    private Integer durationMinutes;

    @Setter(AccessLevel.PACKAGE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false, foreignKey = @ForeignKey(name = "fk_lessons_module"))
    private CourseModule module;

    @OneToMany(mappedBy = "lesson", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<Assignment> assignments = new ArrayList<>();

    public Lesson(String title) {
        this.title = title;
    }

    public void addAssignment(Assignment assignment) {
        if (assignment.getLesson() != null) {
            throw new IllegalArgumentException("Элемент уже принадлежит родителю");
        }
        assignment.setLesson(this);
        assignments.add(assignment);
    }

    public void removeAssignment(Assignment assignment) {
        int index = assignments.indexOf(assignment);
        if (index >= 0) {
            assignments.remove(index).setLesson(null);
        }
    }

    public List<Assignment> getAssignments() {
        return Collections.unmodifiableList(assignments);
    }
}
