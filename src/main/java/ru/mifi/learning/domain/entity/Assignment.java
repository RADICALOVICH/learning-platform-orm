package ru.mifi.learning.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
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
        name = "assignments",
        indexes = {@Index(name = "idx_assignments_lesson", columnList = "lesson_id")})
@Check(constraints = "max_score > 0")
public class Assignment extends BaseEntity {
    @Setter
    @Column(length = 200, nullable = false)
    private String title;

    @Setter
    @Column(columnDefinition = "text", nullable = false)
    private String description;

    @Setter
    private Instant dueDate;

    @Setter
    @Column(nullable = false)
    private int maxScore = 100;

    @Setter(AccessLevel.PACKAGE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false, foreignKey = @ForeignKey(name = "fk_assignments_lesson"))
    private Lesson lesson;

    @OneToMany(mappedBy = "assignment", fetch = FetchType.LAZY)
    private List<Submission> submissions = new ArrayList<>();

    public Assignment(String title, String description) {
        this.title = title;
        this.description = description;
    }

    void addSubmission(Submission submission) {
        submissions.add(submission);
    }

    public List<Submission> getSubmissions() {
        return Collections.unmodifiableList(submissions);
    }
}
