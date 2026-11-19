package ru.mifi.learning.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.mifi.learning.domain.enums.CourseStatus;
import ru.mifi.learning.domain.enums.Role;
import ru.mifi.learning.exception.BusinessRuleException;

class CourseTest {

    @Test
    @DisplayName("При смене преподавателя обе стороны связи отражают изменение")
    void moveTeacher() {
        User teacher = new User("first@example.test", "Ирина", Role.TEACHER);
        User next = new User("next@example.test", "Олег", Role.TEACHER);
        Category category = new Category("Java");
        Course course = new Course("ORM", category, teacher);
        assertThat(teacher.getCoursesTaught()).containsExactly(course);
        course.changeTeacher(next);
        course.changeTeacher(next);
        assertThat(teacher.getCoursesTaught()).isEmpty();
        assertThat(next.getCoursesTaught()).containsExactly(course);
        assertThat(course.getTeacher()).isSameAs(next);
    }

    @Test
    @DisplayName("Модуль без явного порядка получает следующий индекс после максимального")
    void appendAfterLargestIndex() {
        Course course = new Course("ORM", new Category("Java"), new User("t@example.test", "Ирина", Role.TEACHER));
        CourseModule first = new CourseModule("Введение");
        first.setOrderIndex(8);
        course.addModule(first);
        CourseModule second = new CourseModule("Практика");
        course.addModule(second);
        assertThat(second.getOrderIndex()).isEqualTo(9);
        assertThat(second.getCourse()).isSameAs(course);
        course.removeModule(first);
        assertThat(first.getCourse()).isNull();
        assertThat(course.getModules()).containsExactly(second);
    }

    @Test
    @DisplayName("Архивный курс нельзя снова опубликовать")
    void publish() {
        Course course = new Course("ORM", new Category("Java"), new User("t@example.test", "Ирина", Role.TEACHER));
        course.archive();
        assertThatThrownBy(course::publish).isInstanceOf(BusinessRuleException.class);
        assertThat(course.getStatus()).isEqualTo(CourseStatus.ARCHIVED);
    }
}
