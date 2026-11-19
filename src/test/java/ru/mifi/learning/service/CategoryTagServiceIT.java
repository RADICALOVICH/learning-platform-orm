package ru.mifi.learning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;
import ru.mifi.learning.dto.category.CreateCategoryRequest;
import ru.mifi.learning.dto.category.UpdateCategoryRequest;
import ru.mifi.learning.dto.tag.CreateTagRequest;
import ru.mifi.learning.exception.ConflictException;
import ru.mifi.learning.exception.NotFoundException;
import ru.mifi.learning.repository.CourseRepository;
import ru.mifi.learning.repository.TagRepository;
import ru.mifi.learning.support.AbstractIntegrationTest;

class CategoryTagServiceIT extends AbstractIntegrationTest {
    @Autowired
    CategoryService categories;

    @Autowired
    TagService tags;

    @Autowired
    CourseRepository courses;

    @Autowired
    TagRepository tagRepository;

    @Autowired
    TransactionTemplate transactions;

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
