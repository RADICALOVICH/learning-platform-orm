package ru.mifi.learning.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.mifi.learning.domain.enums.Role;
import ru.mifi.learning.exception.BusinessRuleException;

class SubmissionTest {

    @Test
    @DisplayName("Сдача ровно в дедлайн считается своевременной")
    void deadline() {
        Assignment work = new Assignment("Практика", "Условие");
        Instant deadline = Instant.parse("2026-05-01T12:00:00Z");
        work.setDueDate(deadline);
        User student = new User("s@example.test", "Студент", Role.STUDENT);
        assertThat(new Submission(work, student, "Ответ", deadline).isLate()).isFalse();
        assertThat(new Submission(work, student, "Ответ", deadline.plusSeconds(1)).isLate())
                .isTrue();
    }

    @Test
    @DisplayName("Некорректная переоценка сохраняет предыдущий результат")
    void grade() {
        Assignment work = new Assignment("Практика", "Условие");
        work.setMaxScore(4);
        Instant now = Instant.parse("2026-05-01T12:00:00Z");
        Submission submission = new Submission(work, new User("s@example.test", "Студент", Role.STUDENT), "Ответ", now);
        submission.grade(4, "Принято", now);
        assertThatThrownBy(() -> submission.grade(5, "Ошибка", now.plusSeconds(1)))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(submission.getScore()).isEqualTo(4);
        assertThat(submission.getFeedback()).isEqualTo("Принято");
        assertThat(submission.getGradedAt()).isEqualTo(now);
    }
}
