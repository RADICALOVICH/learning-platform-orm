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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "quiz_submissions",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_quiz_submissions_attempt",
                    columnNames = {"quiz_id", "student_id", "attempt_number"})
        },
        indexes = {@Index(name = "idx_quiz_submissions_student", columnList = "student_id")})
@Check(constraints = "score_percent between 0 and 100")
public class QuizSubmission extends BaseEntity {
    @Column(nullable = false)
    private int attemptNumber;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private int maxScore;

    @Column(nullable = false)
    private int scorePercent;

    @Column(nullable = false)
    private boolean passed;

    @Column(nullable = false)
    private Instant takenAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false, foreignKey = @ForeignKey(name = "fk_quiz_submissions_quiz"))
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false, foreignKey = @ForeignKey(name = "fk_quiz_submissions_student"))
    private User student;

    @OneToMany(mappedBy = "quizSubmission", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuizAnswer> answers = new ArrayList<>();

    public QuizSubmission(
            Quiz quiz,
            User student,
            int attemptNumber,
            int score,
            int maxScore,
            int scorePercent,
            boolean passed,
            Instant takenAt) {
        this.quiz = quiz;
        this.student = student;
        this.attemptNumber = attemptNumber;
        this.score = score;
        this.maxScore = maxScore;
        this.scorePercent = scorePercent;
        this.passed = passed;
        this.takenAt = takenAt;
    }

    public void addAnswer(QuizAnswer answer) {
        if (answer.getQuizSubmission() != null) {
            throw new IllegalArgumentException("Элемент уже принадлежит родителю");
        }
        answer.setQuizSubmission(this);
        answers.add(answer);
    }

    public void removeAnswer(QuizAnswer answer) {
        int index = answers.indexOf(answer);
        if (index >= 0) {
            answers.remove(index).setQuizSubmission(null);
        }
    }

    public List<QuizAnswer> getAnswers() {
        return Collections.unmodifiableList(answers);
    }
}
