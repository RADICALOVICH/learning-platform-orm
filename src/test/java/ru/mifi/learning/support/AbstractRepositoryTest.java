package ru.mifi.learning.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestDataFactory.class, QueryCounter.class})
public abstract class AbstractRepositoryTest {
    @Autowired
    protected TestEntityManager em;

    @Autowired
    protected TestDataFactory data;

    @Autowired
    protected QueryCounter queries;

    protected void flushAndClear() {
        em.flush();
        em.clear();
    }
}
