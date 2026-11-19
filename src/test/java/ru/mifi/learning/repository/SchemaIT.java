package ru.mifi.learning.repository;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.mifi.learning.support.AbstractRepositoryTest;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
class SchemaIT extends AbstractRepositoryTest {
    private final JdbcTemplate jdbc;

    @Test
    @DisplayName("Созданы все 18 таблиц сущностей и две таблицы связей")
    void tables() {
        var tables = jdbc.queryForList(
                "select table_name from information_schema.tables where table_schema='public' and table_type='BASE TABLE'",
                String.class);
        assertThat(tables)
                .containsExactlyInAnyOrder(
                        "users",
                        "profiles",
                        "categories",
                        "tags",
                        "courses",
                        "course_tags",
                        "course_modules",
                        "lessons",
                        "assignments",
                        "submissions",
                        "enrollments",
                        "lesson_progress",
                        "course_reviews",
                        "quizzes",
                        "questions",
                        "answer_options",
                        "quiz_submissions",
                        "quiz_answers",
                        "quiz_answer_options",
                        "notifications");
    }

    @Test
    @DisplayName("Имена и состав уникальных ограничений совпадают с моделью")
    void uniqueKeys() {
        var keys = jdbc.queryForList("""
                select (case when tc.table_name = 'quizzes' then tc.table_name else tc.constraint_name end)
                       || ':' || string_agg(k.column_name, ',' order by k.ordinal_position)
                from information_schema.table_constraints tc join information_schema.key_column_usage k
                using (constraint_schema, table_name, constraint_name)
                where tc.table_schema='public' and tc.constraint_type='UNIQUE'
                group by tc.table_name, tc.constraint_name
                """, String.class);
        assertThat(keys)
                .containsExactlyInAnyOrder(
                        "uk_users_email:email",
                        "uk_categories_name:name",
                        "uk_tags_name:name",
                        "uk_enrollments_student_course:student_id,course_id",
                        "uk_submissions_assignment_student:assignment_id,student_id",
                        "uk_course_reviews_course_student:course_id,student_id",
                        "quizzes:module_id",
                        "uk_quiz_submissions_attempt:quiz_id,student_id,attempt_number",
                        "uk_quiz_answers_submission_question:quiz_submission_id,question_id");
    }

    @Test
    @DisplayName("Первичный ключ прогресса и обе таблицы связей используют пары колонок")
    void keys() {
        var keys = jdbc.queryForList("""
                select tc.table_name || ':' || string_agg(k.column_name, ',' order by k.ordinal_position)
                from information_schema.table_constraints tc join information_schema.key_column_usage k
                using (constraint_schema, table_name, constraint_name)
                where tc.table_schema='public' and tc.constraint_type='PRIMARY KEY'
                  and tc.table_name in ('lesson_progress','course_tags','quiz_answer_options','profiles')
                group by tc.table_name
                """, String.class);
        assertThat(keys)
                .containsExactlyInAnyOrder(
                        "lesson_progress:enrollment_id,lesson_id",
                        "course_tags:course_id,tag_id",
                        "quiz_answer_options:answer_option_id,quiz_answer_id",
                        "profiles:user_id");
    }

    @Test
    @DisplayName("Каскадное удаление в БД включено ровно для шести связей")
    void cascade() {
        var cascades = jdbc.queryForList("""
                select constraint_name from information_schema.referential_constraints
                where constraint_schema='public' and delete_rule='CASCADE'
                """, String.class);
        assertThat(cascades)
                .containsExactlyInAnyOrder(
                        "fk_profiles_user",
                        "fk_quizzes_module",
                        "fk_questions_quiz",
                        "fk_answer_options_question",
                        "fk_lesson_progress_lesson",
                        "fk_notifications_recipient");
    }

    @Test
    @DisplayName("У каждого FK есть имя и индекс с FK в первой позиции")
    void fkIndexes() {
        var missing = jdbc.queryForList("""
                select c.conname from pg_constraint c where c.contype='f' and c.connamespace='public'::regnamespace
                  and (left(c.conname, 3) <> 'fk_' or not exists (
                    select 1 from pg_index i where i.indrelid=c.conrelid and i.indisvalid
                        and i.indpred is null and i.indkey[0]=c.conkey[1]))
                """, String.class);
        assertThat(missing).isEmpty();
    }

    @Test
    @DisplayName("Типы, статусы и рейтинг защищены CHECK в PostgreSQL")
    void checks() {
        var checkedColumns = jdbc.queryForList("""
                select distinct t.relname || '.' || a.attname
                from pg_constraint c join pg_class t on t.oid=c.conrelid
                join pg_attribute a on a.attrelid=t.oid and a.attnum=any(c.conkey)
                where c.contype='c' and c.connamespace='public'::regnamespace
                """, String.class);
        assertThat(checkedColumns)
                .contains(
                        "users.role",
                        "courses.status",
                        "enrollments.status",
                        "questions.type",
                        "notifications.type",
                        "submissions.status",
                        "course_reviews.rating");
    }

    @Test
    @DisplayName("16 генерируемых идентификаторов используют последовательности с шагом 50")
    void sequences() {
        var steps = jdbc.queryForList("select increment_by from pg_sequences where schemaname='public'", Long.class);
        assertThat(steps).hasSize(16).containsOnly(50L);
    }
}
