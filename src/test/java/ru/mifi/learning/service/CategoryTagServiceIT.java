package ru.mifi.learning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import ru.mifi.learning.dto.category.CreateCategoryRequest;
import ru.mifi.learning.dto.category.UpdateCategoryRequest;
import ru.mifi.learning.dto.course.CourseTagsRequest;
import ru.mifi.learning.dto.tag.CreateTagRequest;
import ru.mifi.learning.exception.ConflictException;
import ru.mifi.learning.exception.NotFoundException;
import ru.mifi.learning.repository.CourseRepository;
import ru.mifi.learning.repository.TagRepository;
import ru.mifi.learning.support.AbstractIntegrationTest;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
class CategoryTagServiceIT extends AbstractIntegrationTest {
    private final CategoryService categories;
    private final TagService tags;
    private final CourseRepository courses;
    private final TagRepository tagRepository;
    private final TransactionTemplate transactions;

    @Test
    @DisplayName("Категория читается, обновляется и удаляется, повтор имени блокируется")
    void categoryLifecycle() {
        var created = categories.create(new CreateCategoryRequest("Java", "Курсы"));
        assertThat(categories.list()).containsExactly(created);
        assertThatThrownBy(() -> categories.create(new CreateCategoryRequest("java", null)))
                .isInstanceOf(ConflictException.class);
        var updated = categories.update(created.id(), new UpdateCategoryRequest("Java ORM", null));
        assertThat(categories.get(created.id())).isEqualTo(updated);
        var other = categories.create(new CreateCategoryRequest("Другая", null));
        assertThatThrownBy(() -> categories.update(other.id(), new UpdateCategoryRequest("JAVA ORM", null)))
                .isInstanceOf(ConflictException.class);
        categories.delete(created.id());
        assertThatThrownBy(() -> categories.get(created.id())).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Категория с курсом защищена от удаления")
    void categoryInUse() {
        long id = data.publishedCourseWithLessons(1).getCategory().getId();
        assertThatThrownBy(() -> categories.delete(id))
                .isInstanceOf(ConflictException.class)
                .extracting("code")
                .isEqualTo("CATEGORY_IN_USE");
    }

    @Test
    @DisplayName("Два курса одновременно получают один новый тег")
    void concurrentCourseTags(@Autowired CourseService courseService, @Autowired JdbcTemplate jdbc) throws Exception {
        long firstId = data.publishedCourseWithLessons(1).getId();
        long secondId = data.publishedCourseWithLessons(1).getId();
        try (var worker = Executors.newSingleThreadExecutor()) {
            var second = transactions.execute(tx -> {
                var blocker = jdbc.queryForObject("select pg_backend_pid()", Integer.class);
                courseService.tags(firstId, new CourseTagsRequest(List.of(" JPA ")));
                var waiting = worker.submit(() -> courseService.tags(secondId, new CourseTagsRequest(List.of("jpa"))));
                // Фиксируем первый тег только после того, как вторая транзакция попыталась вставить его.
                await().atMost(Duration.ofSeconds(10))
                        .until(() -> jdbc.queryForObject(
                                "select exists(select 1 from pg_stat_activity where ? = any(pg_blocking_pids(pid)))",
                                Boolean.class,
                                blocker));
                return waiting;
            });
            assertThat(second.get(10, TimeUnit.SECONDS).tags()).containsExactly("jpa");
        }
        assertThat(courseService.get(firstId).tags()).containsExactly("jpa");
        assertThat(tags.list()).extracting("name").containsExactly("jpa");
    }

    @Test
    @DisplayName("Теги нормализуются и отвязываются от каждого курса перед удалением")
    void unlinkTag() {
        var tag = tags.create(new CreateTagRequest(" JPA "));
        var keep = tags.create(new CreateTagRequest("ORM"));
        assertThat(tag.name()).isEqualTo("jpa");
        assertThatThrownBy(() -> tags.create(new CreateTagRequest("JpA"))).isInstanceOf(ConflictException.class);
        var first = data.publishedCourseWithLessons(1);
        var second = data.publishedCourseWithLessons(1);
        transactions.executeWithoutResult(tx -> {
            for (var id : List.of(first.getId(), second.getId())) {
                var course = courses.findById(id).orElseThrow();
                course.addTag(tagRepository.findById(tag.id()).orElseThrow());
                course.addTag(tagRepository.findById(keep.id()).orElseThrow());
            }
        });
        tags.delete(tag.id());
        assertThat(tags.list()).containsExactly(keep);
        for (var id : List.of(first.getId(), second.getId())) {
            assertThat(courses.findDetailsById(id).orElseThrow().getTags())
                    .extracting("id")
                    .containsExactly(keep.id());
        }
        assertThatThrownBy(() -> tags.delete(tag.id())).isInstanceOf(NotFoundException.class);
    }
}
