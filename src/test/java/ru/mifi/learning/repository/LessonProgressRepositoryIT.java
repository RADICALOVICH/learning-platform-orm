package ru.mifi.learning.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import ru.mifi.learning.domain.entity.Course;
import ru.mifi.learning.domain.entity.CourseModule;
import ru.mifi.learning.domain.entity.Enrollment;
import ru.mifi.learning.domain.entity.Lesson;
import ru.mifi.learning.domain.entity.LessonProgress;
import ru.mifi.learning.domain.entity.LessonProgressId;
import ru.mifi.learning.support.AbstractRepositoryTest;
import ru.mifi.learning.support.TestDataFactory;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
class LessonProgressRepositoryIT extends AbstractRepositoryTest {
    private final LessonProgressRepository progress;

    @Test
    @DisplayName("Составной первичный ключ исключает повтор пары запись–урок")
    void duplicate() {
        Course course = data.publishedCourseWithLessons(1);
        Enrollment e = data.enrollment(course);
        Lesson lesson = course.getModules().getFirst().getLessons().getFirst();
        progress.saveAndFlush(new LessonProgress(e, lesson, TestDataFactory.NOW));
        em.clear();
        assertThatThrownBy(() -> progress.saveAndFlush(new LessonProgress(
                        em.getEntityManager().getReference(Enrollment.class, e.getId()),
                        em.getEntityManager().getReference(Lesson.class, lesson.getId()),
                        TestDataFactory.NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Поиск по составному ключу сохраняет ленивые ссылки")
    void find() {
        Course course = data.publishedCourseWithLessons(1);
        Enrollment e = data.enrollment(course);
        Lesson lesson = course.getModules().getFirst().getLessons().getFirst();
        LessonProgress entry = new LessonProgress(e, lesson, TestDataFactory.NOW);
        progress.save(entry);
        flushAndClear();
        LessonProgress loaded = progress.findById(new LessonProgressId(e.getId(), lesson.getId()))
                .orElseThrow();
        assertThat(loaded).isEqualTo(entry);
        assertThat(loaded).hasSameHashCodeAs(entry);
        assertThat(Hibernate.isInitialized(loaded.getLesson())).isFalse();
        assertThat(Hibernate.isInitialized(loaded.getEnrollment())).isFalse();
        assertThat(loaded.getLesson().getId()).isEqualTo(lesson.getId());
        assertThat(loaded.getEnrollment().getId()).isEqualTo(e.getId());
    }

    @Test
    @DisplayName("save новой отметки выполняет один INSERT без предварительного SELECT")
    void persist() {
        Course course = data.publishedCourseWithLessons(1);
        Enrollment e = data.enrollment(course);
        Lesson lesson = course.getModules().getFirst().getLessons().getFirst();
        em.flush();
        queries.reset();
        LessonProgress entry = new LessonProgress(e, lesson, TestDataFactory.NOW);
        assertThat(entry.getVersion()).isNull();
        assertThat(progress.saveAndFlush(entry)).isSameAs(entry);
        queries.assertCount(1);
        assertThat(entry.getVersion()).isNotNull();
    }

    @Test
    @DisplayName("Удаление урока каскадно удаляет отметку на уровне БД")
    void deleteLesson() {
        Course course = data.publishedCourseWithLessons(1);
        Enrollment e = data.enrollment(course);
        Lesson lesson = course.getModules().getFirst().getLessons().getFirst();
        LessonProgress entry = progress.save(new LessonProgress(e, lesson, TestDataFactory.NOW));
        flushAndClear();
        CourseModule module =
                em.find(CourseModule.class, course.getModules().getFirst().getId());
        module.removeLesson(module.getLessons().getFirst());
        flushAndClear();
        assertThat(progress.existsById(entry.getId())).isFalse();
    }
}
