package ru.mifi.learning.domain.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import ru.mifi.learning.domain.OrderIndex;
import ru.mifi.learning.exception.BusinessRuleException;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "quizzes")
@Check(constraints = "time_limit_minutes > 0 and passing_score_percent between 0 and 100 and max_attempts > 0")
public class Quiz extends BaseEntity {
    @Setter
    @Column(length = 200, nullable = false)
    private String title;

    @Setter
    @Column(columnDefinition = "text")
    private String description;

    @Setter
    private Integer timeLimitMinutes;

    @Setter
    @Column(nullable = false)
    private int passingScorePercent = 60;

    @Setter
    private Integer maxAttempts;

    @OnDelete(action = OnDeleteAction.CASCADE)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "module_id",
            unique = true,
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_quizzes_module"))
    private CourseModule module;

    @OneToMany(mappedBy = "quiz", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<Question> questions = new ArrayList<>();

    public Quiz(CourseModule module, String title) {
        this.module = module;
        this.title = title;
    }

    public void addQuestion(Question question) {
        if (question.getQuiz() != null) {
            throw new IllegalArgumentException("Элемент уже принадлежит родителю");
        }
        if (question.getOrderIndex() == null) {
            question.setOrderIndex(OrderIndex.after(
                    questions.stream().mapToInt(Question::getOrderIndex).max().orElse(-1)));
        }
        question.setQuiz(this);
        questions.add(question);
    }

    public void removeQuestion(Question question) {
        int index = questions.indexOf(question);
        if (index >= 0) {
            questions.remove(index).setQuiz(null);
        }
    }

    public int totalPoints() {
        long total = questions.stream().mapToLong(Question::getPoints).sum();
        if (total > Integer.MAX_VALUE) {
            throw new BusinessRuleException("INVALID_QUESTION", "Суммарный балл теста слишком велик");
        }
        return (int) total;
    }

    public List<Question> getQuestions() {
        return Collections.unmodifiableList(questions);
    }
}
