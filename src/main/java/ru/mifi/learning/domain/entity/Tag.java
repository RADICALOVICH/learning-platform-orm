package ru.mifi.learning.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ru.mifi.learning.exception.BusinessRuleException;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "tags",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_tags_name",
                    columnNames = {"name"})
        })
public class Tag extends BaseEntity {
    private static final int MAX_NAME_LENGTH = 50;

    @Column(length = MAX_NAME_LENGTH, nullable = false)
    private String name;

    @ManyToMany(mappedBy = "tags", fetch = FetchType.LAZY)
    private Set<Course> courses = new HashSet<>();

    public Tag(String name) {
        setName(name);
    }

    public void setName(String name) {
        this.name = normalizeName(name);
    }

    public static String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessRuleException("INVALID_TAG_NAME", "Заполните имя тега");
        }
        String normalized = name.strip().toLowerCase(Locale.ROOT);
        if (normalized.codePointCount(0, normalized.length()) > MAX_NAME_LENGTH) {
            throw new BusinessRuleException(
                    "INVALID_TAG_NAME",
                    "После приведения к нижнему регистру имя тега должно содержать не более 50 символов");
        }
        if (normalized.indexOf('\0') >= 0) {
            throw new BusinessRuleException("INVALID_TAG_NAME", "Имя тега содержит нулевой символ");
        }
        return normalized;
    }

    void linkCourse(Course course) {
        courses.add(course);
    }

    void unlinkCourse(Course course) {
        courses.remove(course);
    }

    public Set<Course> getCourses() {
        return Collections.unmodifiableSet(courses);
    }
}
