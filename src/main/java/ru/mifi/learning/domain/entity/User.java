package ru.mifi.learning.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.mifi.learning.domain.enums.Role;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_users_email",
                    columnNames = {"email"})
        })
public class User extends BaseEntity {
    @Column(nullable = false)
    private String email;

    @Setter
    @Column(length = 150, nullable = false)
    private String fullName;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    @OneToMany(mappedBy = "teacher", fetch = FetchType.LAZY)
    private List<Course> coursesTaught = new ArrayList<>();

    @OneToMany(mappedBy = "student", fetch = FetchType.LAZY)
    private List<Enrollment> enrollments = new ArrayList<>();

    public User(String email, String fullName, Role role) {
        this.fullName = fullName;
        this.role = role;
        setEmail(email);
    }

    public void setEmail(String email) {
        this.email = email.strip().toLowerCase(Locale.ROOT);
    }

    void addTaughtCourse(Course course) {
        coursesTaught.add(course);
    }

    void removeTaughtCourse(Course course) {
        coursesTaught.remove(course);
    }

    void addEnrollment(Enrollment enrollment) {
        enrollments.add(enrollment);
    }

    public List<Course> getCoursesTaught() {
        return Collections.unmodifiableList(coursesTaught);
    }

    public List<Enrollment> getEnrollments() {
        return Collections.unmodifiableList(enrollments);
    }
}
