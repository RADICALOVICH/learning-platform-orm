package ru.mifi.learning.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;
import ru.mifi.learning.domain.enums.SubmissionStatus;
import ru.mifi.learning.exception.BusinessRuleException;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "submissions",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_submissions_assignment_student",
                    columnNames = {"assignment_id", "student_id"})
        },
        indexes = {@Index(name = "idx_submissions_student", columnList = "student_id")})
@Check(constraints = "score >= 0")
public class Submission extends BaseEntity {
    @Column(columnDefinition = "text", nullable = false)
    private String content;

    @Column(nullable = false)
    private Instant submittedAt;

    @Column(nullable = false)
    private boolean late;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SubmissionStatus status = SubmissionStatus.SUBMITTED;

    private Integer score;

    @Column(columnDefinition = "text")
    private String feedback;

    private Instant gradedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false, foreignKey = @ForeignKey(name = "fk_submissions_assignment"))
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false, foreignKey = @ForeignKey(name = "fk_submissions_student"))
    private User student;

    public Submission(Assignment assignment, User student, String content, Instant submittedAt) {
        this.assignment = assignment;
        this.student = student;
        this.content = content;
        this.submittedAt = submittedAt;
        this.late = assignment.getDueDate() != null && submittedAt.isAfter(assignment.getDueDate());
        assignment.addSubmission(this);
    }

    public void grade(int score, String feedback, Instant now) {
        if (score < 0 || score > assignment.getMaxScore()) {
            throw new BusinessRuleException("SCORE_OUT_OF_RANGE", "Оценка вне допустимого диапазона");
        }
        this.score = score;
        this.feedback = feedback;
        this.gradedAt = now;
        this.status = SubmissionStatus.GRADED;
    }
}
