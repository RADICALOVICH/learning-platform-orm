package ru.mifi.learning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.mifi.learning.dto.review.CreateReviewRequest;
import ru.mifi.learning.dto.review.UpdateReviewRequest;
import ru.mifi.learning.exception.BusinessRuleException;
import ru.mifi.learning.exception.ConflictException;
import ru.mifi.learning.support.AbstractIntegrationTest;

class ReviewServiceIT extends AbstractIntegrationTest {
    @Autowired
    ReviewService reviews;

    @Autowired
    CourseService courses;

    @Test
    @DisplayName("Только записанный студент оставляет один отзыв, средний рейтинг обновляется")
    void review() {
        var c = data.publishedCourseWithLessons(1);
        var module = c.getModules().getFirst();
        var lesson = module.getLessons().getFirst();
        var e = data.enrollment(c);
        long student = e.getStudent().getId();
        assertThatThrownBy(() -> reviews.create(
                        c.getId(), new CreateReviewRequest(data.student().getId(), 3, null)))
                .isInstanceOf(BusinessRuleException.class);
        var r = reviews.create(c.getId(), new CreateReviewRequest(student, 5, "Полезно"));
        assertThatThrownBy(() -> reviews.create(c.getId(), new CreateReviewRequest(student, 4, null)))
                .isInstanceOf(ConflictException.class);
        reviews.update(r.id(), new UpdateReviewRequest(4, null));
        assertThat(courses.get(c.getId()).averageRating()).isEqualTo(4.0);
        reviews.delete(r.id());
        assertThat(courses.get(c.getId()).averageRating()).isNull();
    }
}
