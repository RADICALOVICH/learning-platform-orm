package ru.mifi.learning.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import ru.mifi.learning.domain.entity.Course;
import ru.mifi.learning.domain.entity.Tag;
import ru.mifi.learning.support.AbstractRepositoryTest;

class TagRepositoryIT extends AbstractRepositoryTest {
    @Autowired
    TagRepository tags;

    @Autowired
    CourseRepository courses;

    @Test
    @DisplayName("Связанный тег защищён внешним ключом")
    void restrict() {
        Course course = data.course();
        Tag tag = tags.save(new Tag("jpa"));
        course.addTag(tag);
        courses.save(course);
        flushAndClear();
        tags.deleteById(tag.getId());
        assertThatThrownBy(tags::flush).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("После отвязки на стороне курса тег можно удалить")
    void unlink() {
        Course course = data.course();
        Tag tag = tags.save(new Tag("jpa"));
        course.addTag(tag);
        courses.save(course);
        flushAndClear();
        Tag linked = tags.findWithCoursesById(tag.getId()).orElseThrow();
        Course owner = linked.getCourses().iterator().next();
        owner.removeTag(linked);
        assertThat(linked.getCourses()).isEmpty();
        tags.delete(linked);
        flushAndClear();
        assertThat(tags.existsById(tag.getId())).isFalse();
        assertThat(courses.existsById(course.getId())).isTrue();
    }
}
