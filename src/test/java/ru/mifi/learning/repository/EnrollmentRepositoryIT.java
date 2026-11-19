package ru.mifi.learning.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import ru.mifi.learning.domain.entity.Course;
import ru.mifi.learning.domain.entity.CourseReview;
import ru.mifi.learning.domain.entity.Enrollment;
import ru.mifi.learning.domain.entity.LessonProgress;
import ru.mifi.learning.support.AbstractRepositoryTest;
import ru.mifi.learning.support.TestDataFactory;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
class EnrollmentRepositoryIT extends AbstractRepositoryTest {
    private final EnrollmentRepository enrollments;

    @Test
    @DisplayName("Повторная запись того же студента на курс запрещена")
    void duplicate() {
        Course course = data.publishedCourseWithLessons(1);
        Enrollment existing = data.enrollment(course);
        em.flush();
        assertThatThrownBy(() ->
                        enrollments.saveAndFlush(new Enrollment(existing.getStudent(), course, TestDataFactory.NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Страница записей загружает студентов и отдельно считает общее число")
    void paged() {
        Course course = data.publishedCourseWithLessons(1);
        data.enrollment(course);
        data.enrollment(course);
        flushAndClear();
        queries.reset();
        var page = enrollments.findWithStudentByCourseId(course.getId(), PageRequest.of(0, 1, Sort.by("id")));
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(1);
        assertThat(Hibernate.isInitialized(page.getContent().getFirst().getStudent()))
                .isTrue();
        queries.assertCount(2);
    }

    @Test
    @DisplayName("Отписка удаляет прогресс и сохраняет отзыв")
    void unsubscribe() {
        Course course = data.publishedCourseWithLessons(1);
        Enrollment e = data.enrollment(course);
        LessonProgress progress = e.markLessonCompleted(
                course.getModules().getFirst().getLessons().getFirst(), TestDataFactory.NOW);
        CourseReview review = em.persist(new CourseReview(course, e.getStudent(), 4));
        flushAndClear();
        enrollments.deleteById(e.getId());
        flushAndClear();
        assertThat(em.find(LessonProgress.class, progress.getId())).isNull();
        assertThat(em.find(CourseReview.class, review.getId())).isNotNull();
    }
}
