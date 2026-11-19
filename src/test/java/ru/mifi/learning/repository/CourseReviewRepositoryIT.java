package ru.mifi.learning.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import ru.mifi.learning.domain.entity.Course;
import ru.mifi.learning.domain.entity.CourseReview;
import ru.mifi.learning.domain.entity.User;
import ru.mifi.learning.support.AbstractRepositoryTest;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
class CourseReviewRepositoryIT extends AbstractRepositoryTest {
    private final CourseReviewRepository reviews;

    @Test
    @DisplayName("PostgreSQL отклоняет рейтинг выше пяти")
    void check() {
        Course c = data.publishedCourseWithLessons(1);
        User s = data.student();
        assertThatThrownBy(() -> reviews.saveAndFlush(new CourseReview(c, s, 6)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Средняя оценка учитывает отзывы только заданного курса")
    void average() {
        Course c = data.publishedCourseWithLessons(1);
        reviews.save(new CourseReview(c, data.student(), 4));
        reviews.save(new CourseReview(c, data.student(), 5));
        Course other = data.publishedCourseWithLessons(1);
        reviews.save(new CourseReview(other, data.student(), 1));
        flushAndClear();
        assertThat(reviews.averageRatingByCourseId(c.getId())).contains(4.5);
    }
}
