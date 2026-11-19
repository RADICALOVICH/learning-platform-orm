package ru.mifi.learning;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import ru.mifi.learning.support.TestcontainersConfiguration;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ApplicationContextIT {
    @Test
    @DisplayName("Контекст приложения выполняет запрос к PostgreSQL")
    void queriesPostgres(@Autowired JdbcTemplate jdbc) {
        assertThat(jdbc.queryForObject("select 1", Integer.class)).isEqualTo(1);
    }
}
