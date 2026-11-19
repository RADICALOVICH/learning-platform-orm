package ru.mifi.learning.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import ru.mifi.learning.dto.category.CreateCategoryRequest;
import ru.mifi.learning.service.CategoryService;
import ru.mifi.learning.support.AbstractIntegrationTest;

@TestPropertySource(properties = "app.seed.enabled=true")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class DemoDataSeederIT extends AbstractIntegrationTest {
    private final DemoDataSeeder seeder;
    private final JdbcTemplate jdbc;
    private final CategoryService categories;

    @Test
    @DisplayName("Демо-набор создаёт курсы и учебную историю только при первом запуске")
    void seedOnce() {
        seeder.run(null);
        assertThat(count("users")).isEqualTo(8);
        assertThat(count("profiles")).isEqualTo(8);
        assertThat(count("categories")).isEqualTo(3);
        assertThat(count("tags")).isEqualTo(6);
        assertThat(count("courses")).isEqualTo(4);
        assertThat(count("quizzes")).isEqualTo(2);
        assertThat(count("questions")).isEqualTo(6);
        assertThat(count("assignments")).isEqualTo(2);
        assertThat(count("quiz_submissions")).isEqualTo(3);
        assertThat(count("submissions where late")).isOne();
        assertThat(count("course_reviews")).isEqualTo(3);
        assertThat(count("notifications")).isEqualTo(13);
        assertThat(jdbc.queryForList(
                        "select u.email from enrollments e join users u on u.id=e.student_id where e.status='COMPLETED'",
                        String.class))
                .containsExactly("irina@example.com");
        assertThat(count("enrollments where status='ACTIVE'")).isEqualTo(4);
        var ids = jdbc.queryForList("select id from notifications order by id", Long.class);
        seeder.run(null);
        assertThat(count("courses")).isEqualTo(4);
        assertThat(jdbc.queryForList("select id from notifications order by id", Long.class))
                .isEqualTo(ids);
    }

    @Test
    @DisplayName("Существующая категория используется в демо-курсах без изменения описания")
    void existingCategory() {
        var existing = categories.create(new CreateCategoryRequest("DevOps", "Каталог кафедры"));
        seeder.run(null);
        assertThat(count("categories")).isEqualTo(3);
        assertThat(jdbc.queryForObject("select count(*) from courses where category_id=?", Long.class, existing.id()))
                .isOne();
        assertThat(categories.get(existing.id()).description()).isEqualTo("Каталог кафедры");
    }

    @Test
    @DisplayName("При наличии пользователя сидер сохраняет его данные и пропускает заполнение")
    void existingUser() {
        var existing = data.student();
        seeder.run(null);
        assertThat(jdbc.queryForList("select id from users", Long.class)).isEqualTo(List.of(existing.getId()));
        assertThat(count("courses")).isZero();
    }

    private long count(String from) {
        return jdbc.queryForObject("select count(*) from " + from, Long.class);
    }
}
