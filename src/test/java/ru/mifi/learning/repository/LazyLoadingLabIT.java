package ru.mifi.learning.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.LazyInitializationException;
import org.hibernate.loader.MultipleBagFetchException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.support.TransactionTemplate;
import ru.mifi.learning.domain.entity.Course;
import ru.mifi.learning.domain.entity.Tag;
import ru.mifi.learning.service.CourseService;
import ru.mifi.learning.support.AbstractIntegrationTest;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
class LazyLoadingLabIT extends AbstractIntegrationTest {
    private final CourseRepository courses;
    private final CourseModuleRepository modules;
    private final TagRepository tags;
    private final CourseService service;
    private final TransactionTemplate tx;
    private final EntityManager em;

    @Test
    @DisplayName("L1. Отсоединённый курс требует fetch join для чтения модулей")
    void detachedCollection() {
        long id = data.publishedCourseWithLessons(1).getId();
        var detached = courses.findById(id).orElseThrow();
        assertThatThrownBy(() -> detached.getModules().size()).isInstanceOf(LazyInitializationException.class);
        assertThat(courses.findWithModulesById(id).orElseThrow().getModules()).hasSize(1);
    }

    @Test
    @DisplayName("L2. Прокси знает ключ преподавателя, граф загружает его имя")
    void teacherProxy() {
        var fixture = data.publishedCourseWithLessons(1);
        var proxy = courses.findById(fixture.getId()).orElseThrow().getTeacher();
        assertThat(proxy.getId()).isEqualTo(fixture.getTeacher().getId());
        assertThatThrownBy(proxy::getFullName).isInstanceOf(LazyInitializationException.class);
        assertThat(courses.findDetailsById(fixture.getId())
                        .orElseThrow()
                        .getTeacher()
                        .getFullName())
                .isNotBlank();
    }

    @Test
    @DisplayName("L3. Транзакция скрывает ленивые запросы, DTO сохраняет структуру после её закрытия")
    void transactionBoundary() {
        var fixture = data.publishedCourseWithLessons(1);
        data.publishedCourseWithLessons(2);
        tx.executeWithoutResult(status -> {
            queries.reset();
            var loaded = courses.findAll();
            assertThat(loaded).hasSize(2);
            loaded.forEach(c -> assertThat(c.getModules()).hasSize(1));
            queries.assertCount(3);
        });
        var dto = service.structure(fixture.getId());
        queries.reset();
        assertThat(dto.modules().getFirst().lessons()).hasSize(1);
        queries.assertCount(0);
    }

    @Test
    @DisplayName("L4. Граф преподавателей сокращает число SQL с одиннадцати до одного")
    void teachers() {
        for (int i = 0; i < 10; i++) data.publishedCourseWithLessons(1);
        tx.executeWithoutResult(status -> {
            queries.reset();
            courses.findAll()
                    .forEach(c -> assertThat(c.getTeacher().getFullName()).isNotBlank());
            queries.assertCount(11);
        });
        tx.executeWithoutResult(status -> {
            queries.reset();
            courses.findAll((Specification<Course>) null, Pageable.unpaged())
                    .forEach(c -> assertThat(c.getTeacher().getFullName()).isNotBlank());
            queries.assertCount(1);
        });
    }

    @Test
    @DisplayName("L5. Две bag-коллекции загружаются двумя запросами в одном контексте")
    void bags() {
        long id = data.publishedCourseWithLessons(2).getId();
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> em.createQuery(
                                "select c from Course c join fetch c.modules m join fetch m.lessons where c.id=:id",
                                Course.class)
                        .setParameter("id", id)
                        .getResultList()))
                .hasRootCauseInstanceOf(MultipleBagFetchException.class);
        var course = tx.execute(status -> {
            queries.reset();
            var c = courses.findWithModulesById(id).orElseThrow();
            modules.findWithLessonsByCourseId(id);
            queries.assertCount(2);
            return c;
        });
        assertThat(course.getModules().getFirst().getLessons()).hasSize(2);
    }

    @Test
    @DisplayName("L6. Пагинация по курсам с графом to-one и пакетной загрузкой тегов")
    void paging() {
        taggedCourses(3);
        assertThatThrownBy(() -> tx.executeWithoutResult(
                        s -> em.createQuery("select c from Course c left join fetch c.tags", Course.class)
                                .setMaxResults(2)
                                .getResultList()))
                .isInstanceOf(HibernateException.class)
                .hasMessageContaining("collection fetch");
        tx.executeWithoutResult(s -> {
            queries.reset();
            var page = courses.findAll((Specification<Course>) null, PageRequest.of(0, 2));
            assertThat(page.getTotalElements()).isEqualTo(3);
            page.forEach(c -> {
                assertThat(c.getTags()).hasSize(1);
                assertThat(c.getTeacher().getFullName()).isNotBlank();
            });
            queries.assertCount(3);
        });
    }

    @Test
    @DisplayName("L7. Для 25 коллекций тегов с BatchSize 20 достаточно двух дополнительных SQL")
    void batches() {
        taggedCourses(25);
        tx.executeWithoutResult(s -> {
            var loaded = courses.findAll();
            assertThat(loaded).hasSize(25);
            queries.reset();
            loaded.forEach(c -> assertThat(c.getTags()).hasSize(1));
            queries.assertCount(2);
        });
    }

    private void taggedCourses(int count) {
        tx.executeWithoutResult(s -> {
            var tag = tags.save(new Tag("orm"));
            for (int i = 0; i < count; i++) {
                var c = data.course();
                c.addTag(tag);
                courses.save(c);
            }
        });
    }

    @Test
    @DisplayName("L8. Автоматический flush обновляет сущность только в транзакции записи")
    void readOnly() {
        var fixture = data.publishedCourseWithLessons(1);
        long id = fixture.getId();
        var readOnly = new TransactionTemplate(tx.getTransactionManager());
        readOnly.setReadOnly(true);
        queries.reset();
        readOnly.executeWithoutResult(s -> courses.findById(id).orElseThrow().setTitle("Черновик"));
        queries.assertCount(1);
        assertThat(courses.findById(id).orElseThrow().getTitle()).isEqualTo(fixture.getTitle());
        queries.reset();
        tx.executeWithoutResult(s -> courses.findById(id).orElseThrow().setTitle("Практикум"));
        queries.assertCount(2);
        assertThat(courses.findById(id).orElseThrow().getTitle()).isEqualTo("Практикум");
    }

    @Test
    @DisplayName("L9. Ссылка по первичному ключу не требует SELECT")
    void reference() {
        long id = data.publishedCourseWithLessons(1).getId();
        tx.executeWithoutResult(s -> {
            queries.reset();
            var proxy = courses.getReferenceById(id);
            assertThat(proxy.getId()).isEqualTo(id);
            assertThat(Hibernate.isInitialized(proxy)).isFalse();
            queries.assertCount(0);
        });
    }
}
