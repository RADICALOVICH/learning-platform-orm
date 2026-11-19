package ru.mifi.learning.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "answer_options",
        indexes = {@Index(name = "idx_answer_options_question", columnList = "question_id")})
public class AnswerOption extends BaseEntity {
    @Setter
    @Column(length = 500, nullable = false)
    private String text;

    @Setter
    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @Setter(AccessLevel.PACKAGE)
    @ColumnDefault("0")
    @Column(nullable = false)
    private int orderIndex;

    @Setter(AccessLevel.PACKAGE)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false, foreignKey = @ForeignKey(name = "fk_answer_options_question"))
    private Question question;

    public AnswerOption(String text, boolean correct) {
        this.text = text;
        this.correct = correct;
    }
}
