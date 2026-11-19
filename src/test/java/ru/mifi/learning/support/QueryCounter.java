package ru.mifi.learning.support;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

public class QueryCounter {
    private final Statistics statistics;

    public QueryCounter(EntityManagerFactory emf) {
        statistics = emf.unwrap(SessionFactory.class).getStatistics();
    }

    public void reset() {
        statistics.clear();
    }

    public long count() {
        return statistics.getPrepareStatementCount();
    }

    public void assertCount(long expected) {
        assertThat(count()).as("Число подготовленных SQL-запросов").isEqualTo(expected);
    }
}
