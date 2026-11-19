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
    @Autowired
    protected TestDataFactory data;

    @Autowired
    protected QueryCounter queries;

    @BeforeEach
    void clearDatabase(@Autowired DatabaseCleaner cleaner) {
        cleaner.clean();
    }
}
