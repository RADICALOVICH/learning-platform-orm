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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import ru.mifi.learning.domain.enums.QuestionType;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "questions",
        indexes = {@Index(name = "idx_questions_quiz", columnList = "quiz_id")})
@Check(constraints = "points > 0")
public class Question extends BaseEntity {
    @Setter
    @Column(columnDefinition = "text", nullable = false)
    private String text;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private QuestionType type;

    @Setter
    @Column(nullable = false)
    private Integer orderIndex;

    @Setter
    @Column(nullable = false)
    private int points = 1;

    @Setter(AccessLevel.PACKAGE)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false, foreignKey = @ForeignKey(name = "fk_questions_quiz"))
    private Quiz quiz;

    @OneToMany(mappedBy = "question", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC, id ASC")
    @BatchSize(size = 50)
    private List<AnswerOption> options = new ArrayList<>();

    public Question(String text, QuestionType type) {
        this.text = text;
        this.type = type;
    }

    public void addOption(AnswerOption option) {
        if (option.getQuestion() != null) {
            throw new IllegalArgumentException("Элемент уже принадлежит родителю");
        }
        option.setQuestion(this);
        option.setOrderIndex(options.isEmpty() ? 0 : options.getLast().getOrderIndex() + 1);
        options.add(option);
    }

    public void replaceOptions(List<AnswerOption> ordered) {
        if (ordered.equals(options)) {
            return;
        }
        options.forEach(option -> option.setQuestion(null));
        options.clear();
        ordered.forEach(this::addOption);
    }

    public void removeOption(AnswerOption option) {
        int index = options.indexOf(option);
        if (index >= 0) {
            options.remove(index).setQuestion(null);
        }
    }

    public Set<Long> correctOptionIds() {
        return options.stream()
                .filter(AnswerOption::isCorrect)
                .map(AnswerOption::getId)
                .collect(Collectors.toUnmodifiableSet());
    }

    public List<AnswerOption> getOptions() {
        return Collections.unmodifiableList(options);
    }
}
