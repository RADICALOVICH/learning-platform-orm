package ru.mifi.learning.support;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;

@RequiredArgsConstructor
public class DatabaseCleaner {
    private final JdbcTemplate jdbc;

    public void clean() {
        String tables = jdbc.queryForObject("""
                select string_agg(format('%I.%I', schemaname, tablename), ', ' order by tablename)
                from pg_tables where schemaname = 'public'
                """, String.class);
        if (tables != null) {
            jdbc.execute("truncate table " + tables + " cascade");
        }
    }
}
