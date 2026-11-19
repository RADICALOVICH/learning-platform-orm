package ru.mifi.learning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import ru.mifi.learning.domain.entity.Course;
import ru.mifi.learning.domain.enums.CourseStatus;
import ru.mifi.learning.dto.course.CourseTagsRequest;
import ru.mifi.learning.dto.course.CreateCourseRequest;
import ru.mifi.learning.dto.course.LessonRequest;
import ru.mifi.learning.dto.course.ModuleInput;
import ru.mifi.learning.dto.course.UpdateCourseRequest;
import ru.mifi.learning.exception.BusinessRuleException;
import ru.mifi.learning.exception.ConflictException;
import ru.mifi.learning.exception.NotFoundException;
import ru.mifi.learning.support.AbstractIntegrationTest;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
class CourseServiceIT extends AbstractIntegrationTest {
    private final CourseService courses;

    private CreateCourseRequest request(Course template, long teacher) {
        return new CreateCourseRequest(
                "ORM",
                null,
                template.getCategory().getId(),
                teacher,
                null,
                null,
                List.of("ORM", "java", "orm"),
                List.of(new ModuleInput(
                        "Раздел", null, null, List.of(new LessonRequest("Урок", null, null, null, null)))));
    }

    @Test
    @DisplayName("Переполнение индекса вложенного модуля или урока отменяет создание всего курса")
    void nestedOrderLimit() {
        var template = data.course();
        var last = new LessonRequest("Последний", null, null, null, Integer.MAX_VALUE);
        var next = new LessonRequest("Следующий", null, null, null, null);
        for (var modules : List.of(
                List.of(
                        new ModuleInput("Последний", null, Integer.MAX_VALUE, List.of()),
                        new ModuleInput("Следующий", null, null, List.of())),
                List.of(new ModuleInput("Раздел", null, null, List.of(last, next))))) {
            var request = new CreateCourseRequest(
                    "Переполнение",
                    null,
                    template.getCategory().getId(),
                    template.getTeacher().getId(),
                    null,
                    null,
                    List.of(),
                    modules);
            assertThatThrownBy(() -> courses.create(request)).extracting("code").isEqualTo("ORDER_INDEX_EXHAUSTED");
        }
        assertThat(courses.list(null, null, null, null, "Переполнение", PageRequest.of(0, 5))
                        .totalElements())
                .isZero();
    }

    @Test
    @DisplayName("Карточка читается одним запросом, структура тремя")
    void createRead() {
        var template = data.course();
        var resp = courses.create(request(template, template.getTeacher().getId()));
        queries.reset();
        var card = courses.get(resp.id());
        queries.assertCount(1);
        assertThat(card.tags()).containsExactly("java", "orm");
        assertThat(card.moduleCount()).isOne();
        assertThat(card.studentCount()).isZero();
        assertThat(card.averageRating()).isNull();
        queries.reset();
        var structure = courses.structure(resp.id());
        queries.assertCount(3);
        assertThat(structure.modules().getFirst().lessons().getFirst().title()).isEqualTo("Урок");
    }

    @Test
    @DisplayName("Студент не может вести новый курс")
    void teacher() {
        var template = data.course();
        assertThatThrownBy(() -> courses.create(request(template, data.student().getId())))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code")
                .isEqualTo("USER_NOT_TEACHER");
    }

    @Test
    @DisplayName("Каталог фильтрует и загружает страницу без N+1")
    void catalog() {
        var t = data.course();
        for (int i = 0; i < 3; i++) {
            courses.create(request(t, t.getTeacher().getId()));
        }
        queries.reset();
        var page = courses.list(
                t.getCategory().getId(),
                t.getTeacher().getId(),
                "ORM",
                CourseStatus.DRAFT,
                "orm",
                PageRequest.of(0, 2, Sort.by("id")));
        queries.assertCount(3);
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.content())
                .hasSize(2)
                .allSatisfy(row -> assertThat(row.tags()).containsExactly("java", "orm"));
    }

    @Test
    @DisplayName("Курс можно обновить, опубликовать, архивировать и удалить")
    void edit() {
        var t = data.course();
        long id = courses.create(request(t, t.getTeacher().getId())).id();
        long teacher = data.teacher().getId();
        assertThat(courses.update(
                                id,
                                new UpdateCourseRequest(
                                        "Новое", "Описание", t.getCategory().getId(), teacher, null, 2))
                        .teacher()
                        .id())
                .isEqualTo(teacher);
        assertThat(courses.tags(id, new CourseTagsRequest(List.of("One"))).tags())
                .containsExactly("one");
        assertThat(courses.publish(id).status()).isEqualTo(CourseStatus.PUBLISHED);
        assertThat(courses.archive(id).status()).isEqualTo(CourseStatus.ARCHIVED);
        courses.delete(id);
        assertThatThrownBy(() -> courses.get(id)).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Пустой курс нельзя публиковать, курс с записью нельзя удалить")
    void protectedCourse() {
        var t = data.course();
        var id = courses.create(new CreateCourseRequest(
                        "Пустой", null, t.getCategory().getId(), t.getTeacher().getId(), null, null, null, null))
                .id();
        assertThatThrownBy(() -> courses.publish(id)).isInstanceOf(BusinessRuleException.class);
        assertThat(courses.get(id).status()).isEqualTo(CourseStatus.DRAFT);
        var c = data.publishedCourseWithLessons(1);
        data.enrollment(c);
        assertThatThrownBy(() -> courses.delete(c.getId()))
                .isInstanceOf(ConflictException.class)
                .extracting("code")
                .isEqualTo("COURSE_HAS_LEARNING_DATA");
    }
}
