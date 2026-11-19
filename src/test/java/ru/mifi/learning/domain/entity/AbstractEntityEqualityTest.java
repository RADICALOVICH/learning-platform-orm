package ru.mifi.learning.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AbstractEntityEqualityTest {

    @Test
    @DisplayName("Два новых тега с одинаковым именем имеют разную идентичность")
    void transientIdentity() {
        Tag first = new Tag("orm");
        assertThat(first).isEqualTo(first).isNotEqualTo(new Tag("orm"));
    }

    @Test
    @DisplayName("Составной ключ сравнивается по двум значениям")
    void compositeKeys() {
        LessonProgressId id = new LessonProgressId(3L, 7L);
        assertThat(id).isEqualTo(new LessonProgressId(3L, 7L)).hasSameHashCodeAs(new LessonProgressId(3L, 7L));
        assertThat(id).isNotEqualTo(new LessonProgressId(3L, 8L)).isNotEqualTo(new LessonProgressId(4L, 7L));
    }
}
