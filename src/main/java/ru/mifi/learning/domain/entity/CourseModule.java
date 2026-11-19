package ru.mifi.learning.domain.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.mifi.learning.domain.OrderIndex;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "course_modules",
        indexes = {@Index(name = "idx_course_modules_course", columnList = "course_id")})
public class CourseModule extends BaseEntity {
    @Setter
    @Column(length = 200, nullable = false)
    private String title;

    @Setter
    @Column(columnDefinition = "text")
    private String description;

    @Setter
    @Column(nullable = false)
    private Integer orderIndex;

    @Setter(AccessLevel.PACKAGE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false, foreignKey = @ForeignKey(name = "fk_course_modules_course"))
    private Course course;

    @OneToMany(mappedBy = "module", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<Lesson> lessons = new ArrayList<>();

    public CourseModule(String title) {
        this.title = title;
    }

    public void addLesson(Lesson lesson) {
        if (lesson.getModule() != null) {
            throw new IllegalArgumentException("Элемент уже принадлежит родителю");
        }
        if (lesson.getOrderIndex() == null) {
            lesson.setOrderIndex(OrderIndex.after(
                    lessons.stream().mapToInt(Lesson::getOrderIndex).max().orElse(-1)));
        }
        lesson.setModule(this);
        lessons.add(lesson);
    }

    public void removeLesson(Lesson lesson) {
        int index = lessons.indexOf(lesson);
        if (index >= 0) {
            lessons.remove(index).setModule(null);
        }
    }

    public List<Lesson> getLessons() {
        return Collections.unmodifiableList(lessons);
    }
}
