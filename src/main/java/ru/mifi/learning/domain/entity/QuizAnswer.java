package ru.mifi.learning.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "quiz_answers",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_quiz_answers_submission_question",
                    columnNames = {"quiz_submission_id", "question_id"})
        },
        indexes = {@Index(name = "idx_quiz_answers_question", columnList = "question_id")})
public class QuizAnswer extends BaseEntity {
    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @Column(nullable = false)
    private int pointsAwarded;

    @Setter(AccessLevel.PACKAGE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "quiz_submission_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_quiz_answers_quiz_submission"))
    private QuizSubmission quizSubmission;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false, foreignKey = @ForeignKey(name = "fk_quiz_answers_question"))
    private Question question;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "quiz_answer_options",
            joinColumns = @JoinColumn(name = "quiz_answer_id"),
            inverseJoinColumns = @JoinColumn(name = "answer_option_id"),
            foreignKey = @ForeignKey(name = "fk_quiz_answer_options_quiz_answer"),
            inverseForeignKey = @ForeignKey(name = "fk_quiz_answer_options_answer_option"),
            indexes = @Index(name = "idx_quiz_answer_options_quiz_answer", columnList = "quiz_answer_id"))
    private Set<AnswerOption> selectedOptions = new HashSet<>();

    public QuizAnswer(Question question, boolean correct, int pointsAwarded, Collection<AnswerOption> selectedOptions) {
        this.question = question;
        this.correct = correct;
        this.pointsAwarded = pointsAwarded;
        this.selectedOptions.addAll(selectedOptions);
    }

    public Set<AnswerOption> getSelectedOptions() {
        return Collections.unmodifiableSet(selectedOptions);
    }
}
