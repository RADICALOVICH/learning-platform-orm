package ru.mifi.learning.domain.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Check;
import ru.mifi.learning.domain.OrderIndex;
import ru.mifi.learning.domain.enums.CourseStatus;
import ru.mifi.learning.exception.BusinessRuleException;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "courses",
        indexes = {
            @Index(name = "idx_courses_status", columnList = "status"),
            @Index(name = "idx_courses_category", columnList = "category_id"),
            @Index(name = "idx_courses_teacher", columnList = "teacher_id")
        })
@Check(constraints = "duration_weeks > 0")
public class Course extends BaseEntity {
    @Setter
    @Column(length = 200, nullable = false)
    private String title;

    @Setter
    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CourseStatus status = CourseStatus.DRAFT;

    @Setter
    private LocalDate startDate;

    @Setter
    private Integer durationWeeks;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false, foreignKey = @ForeignKey(name = "fk_courses_category"))
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false, foreignKey = @ForeignKey(name = "fk_courses_teacher"))
    private User teacher;

    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<CourseModule> modules = new ArrayList<>();

    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    private List<Enrollment> enrollments = new ArrayList<>();

    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    private List<CourseReview> reviews = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "course_tags",
            joinColumns = @JoinColumn(name = "course_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"),
            foreignKey = @ForeignKey(name = "fk_course_tags_course"),
            inverseForeignKey = @ForeignKey(name = "fk_course_tags_tag"),
            indexes = @Index(name = "idx_course_tags_tag", columnList = "tag_id"))
    @BatchSize(size = 20)
    private Set<Tag> tags = new HashSet<>();

    public Course(String title, Category category, User teacher) {
        this.title = title;
        changeCategory(category);
        changeTeacher(teacher);
    }

    public void addModule(CourseModule module) {
        if (module.getCourse() != null) {
            throw new IllegalArgumentException("Элемент уже принадлежит родителю");
        }
        if (module.getOrderIndex() == null) {
            module.setOrderIndex(OrderIndex.after(
                    modules.stream().mapToInt(CourseModule::getOrderIndex).max().orElse(-1)));
        }
        module.setCourse(this);
        modules.add(module);
    }

    public void removeModule(CourseModule module) {
        int index = modules.indexOf(module);
        if (index >= 0) {
            modules.remove(index).setCourse(null);
        }
    }

    public void addTag(Tag tag) {
        if (tags.add(tag)) {
            tag.linkCourse(this);
        }
    }

    public void removeTag(Tag tag) {
        if (tags.remove(tag)) {
            tag.unlinkCourse(this);
        }
    }

    public void publish() {
        if (status != CourseStatus.DRAFT) {
            throw new BusinessRuleException("INVALID_STATUS_TRANSITION", "Опубликовать можно только черновик курса");
        }
        status = CourseStatus.PUBLISHED;
    }

    public void archive() {
        if (status == CourseStatus.ARCHIVED) {
            throw new BusinessRuleException("INVALID_STATUS_TRANSITION", "Курс уже в архиве");
        }
        status = CourseStatus.ARCHIVED;
    }

    public void changeTeacher(User teacher) {
        Objects.requireNonNull(teacher);
        if (teacher.equals(this.teacher)) {
            return;
        }
        if (this.teacher != null) {
            this.teacher.removeTaughtCourse(this);
        }
        this.teacher = teacher;
        teacher.addTaughtCourse(this);
    }

    public void changeCategory(Category category) {
        Objects.requireNonNull(category);
        if (category.equals(this.category)) {
            return;
        }
        if (this.category != null) {
            this.category.removeCourse(this);
        }
        this.category = category;
        category.addCourse(this);
    }

    void addEnrollment(Enrollment enrollment) {
        enrollments.add(enrollment);
    }

    void addReview(CourseReview review) {
        reviews.add(review);
    }

    public List<CourseModule> getModules() {
        return Collections.unmodifiableList(modules);
    }

    public List<Enrollment> getEnrollments() {
        return Collections.unmodifiableList(enrollments);
    }

    public List<CourseReview> getReviews() {
        return Collections.unmodifiableList(reviews);
    }

    public Set<Tag> getTags() {
        return Collections.unmodifiableSet(tags);
    }
}
