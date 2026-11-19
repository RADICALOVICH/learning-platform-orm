package ru.mifi.learning.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestDataFactory.class, DatabaseCleaner.class, QueryCounter.class})
public abstract class AbstractIntegrationTest {
    protected TestDataFactory data;

    protected QueryCounter queries;

    @BeforeEach
    void setUp(@Autowired DatabaseCleaner cleaner, @Autowired TestDataFactory data, @Autowired QueryCounter queries) {
        this.data = data;
        this.queries = queries;
        cleaner.clean();
    }
}
