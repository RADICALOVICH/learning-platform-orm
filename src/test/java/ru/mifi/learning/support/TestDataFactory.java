package ru.mifi.learning.support;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.mifi.learning.domain.entity.AnswerOption;
import ru.mifi.learning.domain.entity.Category;
import ru.mifi.learning.domain.entity.Course;
import ru.mifi.learning.domain.entity.CourseModule;
import ru.mifi.learning.domain.entity.Enrollment;
import ru.mifi.learning.domain.entity.Lesson;
import ru.mifi.learning.domain.entity.Question;
import ru.mifi.learning.domain.entity.Quiz;
import ru.mifi.learning.domain.entity.User;
import ru.mifi.learning.domain.enums.QuestionType;
import ru.mifi.learning.domain.enums.Role;
import ru.mifi.learning.repository.CategoryRepository;
import ru.mifi.learning.repository.CourseRepository;
import ru.mifi.learning.repository.EnrollmentRepository;
import ru.mifi.learning.repository.QuizRepository;
import ru.mifi.learning.repository.UserRepository;

@Component
@Transactional
@RequiredArgsConstructor
public class TestDataFactory {
    public static final Instant NOW = Instant.parse("2026-01-10T10:00:00Z");
    private final UserRepository users;
    private final CategoryRepository categories;
    private final CourseRepository courses;
    private final EnrollmentRepository enrollments;
    private final QuizRepository quizzes;

    public User student() {
        return user(Role.STUDENT);
    }

    public User teacher() {
        return user(Role.TEACHER);
    }

    private User user(Role role) {
        return users.save(new User(UUID.randomUUID() + "@example.test", "Тестовый пользователь", role));
    }

    public Course course() {
        Category category = categories.save(new Category("Категория " + UUID.randomUUID()));
        return new Course("Основы ORM", category, teacher());
    }

    public Course publishedCourseWithLessons(int count) {
        Course course = course();
        CourseModule module = new CourseModule("Модуль");
        course.addModule(module);
        for (int index = 0; index < count; index++) module.addLesson(new Lesson("Урок " + index));
        course.publish();
        return courses.save(course);
    }

    public Enrollment enrollment(Course course) {
        return enrollments.save(new Enrollment(student(), course, NOW));
    }

    public Quiz quizWithQuestions(CourseModule module, int count) {
        Quiz quiz = new Quiz(module, "Контрольный тест");
        for (int index = 0; index < count; index++) {
            Question question = new Question("Вопрос " + index, QuestionType.SINGLE_CHOICE);
            question.addOption(new AnswerOption("Да", true));
            question.addOption(new AnswerOption("Нет", false));
            quiz.addQuestion(question);
        }
        return quizzes.save(quiz);
    }
}
