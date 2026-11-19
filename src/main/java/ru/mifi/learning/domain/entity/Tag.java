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
    @Column(length = 50, nullable = false)
    private String name;

    @ManyToMany(mappedBy = "tags", fetch = FetchType.LAZY)
    private Set<Course> courses = new HashSet<>();

    public Tag(String name) {
        setName(name);
    }

    public void setName(String name) {
        this.name = name.strip().toLowerCase(Locale.ROOT);
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
