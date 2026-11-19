package ru.mifi.learning.seed;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.mifi.learning.domain.entity.Category;
import ru.mifi.learning.domain.enums.QuestionType;
import ru.mifi.learning.domain.enums.Role;
import ru.mifi.learning.dto.assignment.AssignmentRequest;
import ru.mifi.learning.dto.assignment.GradeRequest;
import ru.mifi.learning.dto.assignment.SubmitRequest;
import ru.mifi.learning.dto.category.CreateCategoryRequest;
import ru.mifi.learning.dto.course.CreateCourseRequest;
import ru.mifi.learning.dto.course.LessonRequest;
import ru.mifi.learning.dto.course.ModuleInput;
import ru.mifi.learning.dto.enrollment.StudentRequest;
import ru.mifi.learning.dto.quiz.CreateQuizRequest;
import ru.mifi.learning.dto.quiz.OptionRequest;
import ru.mifi.learning.dto.quiz.QuestionRequest;
import ru.mifi.learning.dto.quiz.QuizAnswerRequest;
import ru.mifi.learning.dto.quiz.TakeQuizRequest;
import ru.mifi.learning.dto.quiz.TeacherOptionView;
import ru.mifi.learning.dto.quiz.TeacherQuizView;
import ru.mifi.learning.dto.review.CreateReviewRequest;
import ru.mifi.learning.dto.user.CreateUserRequest;
import ru.mifi.learning.dto.user.UpsertProfileRequest;
import ru.mifi.learning.repository.CategoryRepository;
import ru.mifi.learning.repository.UserRepository;
import ru.mifi.learning.service.AssignmentService;
import ru.mifi.learning.service.CategoryService;
import ru.mifi.learning.service.CourseService;
import ru.mifi.learning.service.EnrollmentService;
import ru.mifi.learning.service.ProfileService;
import ru.mifi.learning.service.ProgressService;
import ru.mifi.learning.service.QuizService;
import ru.mifi.learning.service.QuizSubmissionService;
import ru.mifi.learning.service.ReviewService;
import ru.mifi.learning.service.SubmissionService;
import ru.mifi.learning.service.UserService;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final UserRepository userRepository;
    private final UserService users;
    private final ProfileService profiles;
    private final CategoryService categories;
    private final CategoryRepository categoryRepository;
    private final CourseService courses;
    private final EnrollmentService enrollments;
    private final AssignmentService assignments;
    private final SubmissionService submissions;
    private final QuizService quizzes;
    private final QuizSubmissionService attempts;
    private final ProgressService progress;
    private final ReviewService reviews;
    private final Clock clock;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Сериализуем проверку пустой БД и первую запись при одновременном старте приложения.
        jdbc.execute("lock table users in share row exclusive mode");
        if (userRepository.count() > 0) return;

        user("admin", "Ольга Соколова", Role.ADMIN);
        long javaTeacher = user("anna", "Анна Миронова", Role.TEACHER);
        long dbTeacher = user("pavel", "Павел Орлов", Role.TEACHER);
        long irina = user("irina", "Ирина Волкова", Role.STUDENT);
        long maxim = user("maxim", "Максим Власов", Role.STUDENT);
        long vera = user("vera", "Вера Белова", Role.STUDENT);
        long oleg = user("oleg", "Олег Зайцев", Role.STUDENT);
        long sofia = user("sofia", "Софья Лебедева", Role.STUDENT);
        long programming = category("Программирование");
        long databases = category("Базы данных");
        long devops = category("DevOps");
        long orm = course(
                "Основы Hibernate",
                programming,
                javaTeacher,
                List.of("java", "hibernate", "beginner"),
                List.of(
                        module("Модель данных", "Сущности", "Связи"),
                        module("Чтение данных", "JPQL", "Планы загрузки")));
        long spring = course(
                "Spring Boot для начинающих",
                programming,
                javaTeacher,
                List.of("java", "spring", "beginner"),
                List.of(module("Контекст", "Компоненты"), module("Web", "Контроллеры")));
        long postgres = course(
                "PostgreSQL на практике",
                databases,
                dbTeacher,
                List.of("sql"),
                List.of(module("Запросы", "Группировка"), module("Производительность", "Индексы")));
        course("Docker и CI", devops, dbTeacher, List.of("docker"), List.of(module("Контейнеры", "Образы")));
        for (long id : List.of(orm, spring, postgres)) courses.publish(id);
        for (long id : List.of(irina, maxim, vera)) enrollments.enroll(orm, new StudentRequest(id));
        enrollments.enroll(spring, new StudentRequest(oleg));
        enrollments.enroll(postgres, new StudentRequest(sofia));

        seedHibernateActivity(orm, irina, maxim, vera);
        reviews.create(orm, new CreateReviewRequest(irina, 5, "Разобралась с планами загрузки"));
        reviews.create(orm, new CreateReviewRequest(maxim, 4, "Хочется больше задач по JPQL"));
        reviews.create(spring, new CreateReviewRequest(oleg, 5, "Понятный пример контроллера"));
    }

    private void seedHibernateActivity(long orm, long irina, long maxim, long vera) {
        var modules = courses.structure(orm).modules();
        var first = modules.getFirst();
        var second = modules.getLast();
        var modeling = quiz(
                first.id(),
                "Модель ORM",
                List.of(
                        single("Чем обозначают сущность?", "@Entity", "@Table"),
                        single("Как задают первичный ключ?", "@Id", "@Index"),
                        multiple("Выберите связи между сущностями", "@ManyToOne", "@OneToMany", "@Column")));
        var fetching = quiz(
                second.id(),
                "Загрузка данных",
                List.of(
                        single("Какой язык обращается к сущностям?", "JPQL", "DDL"),
                        single("Где находится кэш первого уровня?", "Session", "DataSource"),
                        multiple("Чем можно задать план загрузки?", "JOIN FETCH", "EntityGraph", "GROUP BY")));
        var now = clock.instant();
        var task = assignments.create(
                first.lessons().getFirst().id(),
                new AssignmentRequest("Модель каталога", "Опишите сущность книги", now.plus(Duration.ofDays(7)), 10));
        var lateTask = assignments.create(
                second.lessons().getFirst().id(),
                new AssignmentRequest(
                        "Запрос каталога", "Выберите книги по автору", now.minus(Duration.ofDays(1)), 10));
        var solution = submissions.submit(task.id(), new SubmitRequest(irina, "У книги есть ключ, название и автор."));
        submissions.grade(solution.id(), new GradeRequest(9, "Модель корректна"));
        submissions.submit(lateTask.id(), new SubmitRequest(maxim, "select b from Book b where b.author = :author"));
        for (var module : modules) {
            for (var lesson : module.lessons()) {
                progress.mark(lesson.id(), new StudentRequest(irina));
                progress.mark(lesson.id(), new StudentRequest(maxim));
            }
        }
        progress.mark(first.lessons().getFirst().id(), new StudentRequest(vera));
        pass(modeling, irina);
        pass(fetching, irina);
        attempts.submit(modeling.id(), new TakeQuizRequest(maxim, List.of()));
    }

    private long user(String login, String name, Role role) {
        var user = users.create(new CreateUserRequest(login + "@example.com", name, role));
        profiles.upsert(user.id(), new UpsertProfileRequest(name, null, null, "Москва", null));
        return user.id();
    }

    private long category(String title) {
        return categoryRepository
                .findByNameIgnoreCase(title)
                .map(Category::getId)
                .orElseGet(() -> categories
                        .create(new CreateCategoryRequest(title, null))
                        .id());
    }

    private long course(String title, long category, long teacher, List<String> tags, List<ModuleInput> modules) {
        return courses.create(new CreateCourseRequest(title, null, category, teacher, null, null, tags, modules))
                .id();
    }

    private static ModuleInput module(String title, String... topics) {
        return new ModuleInput(
                title,
                null,
                null,
                List.of(topics).stream()
                        .map(topic -> new LessonRequest(topic, null, null, 20, null))
                        .toList());
    }

    private TeacherQuizView quiz(long module, String title, List<QuestionRequest> questions) {
        return quizzes.create(module, new CreateQuizRequest(title, null, 15, 70, null, questions));
    }

    private static QuestionRequest single(String text, String correct, String wrong) {
        return new QuestionRequest(
                text,
                QuestionType.SINGLE_CHOICE,
                1,
                List.of(new OptionRequest(correct, true), new OptionRequest(wrong, false)));
    }

    private static QuestionRequest multiple(String text, String first, String second, String wrong) {
        return new QuestionRequest(
                text,
                QuestionType.MULTIPLE_CHOICE,
                2,
                List.of(
                        new OptionRequest(first, true),
                        new OptionRequest(second, true),
                        new OptionRequest(wrong, false)));
    }

    private void pass(TeacherQuizView quiz, long student) {
        var answers = quiz.questions().stream()
                .map(q -> new QuizAnswerRequest(
                        q.id(),
                        q.options().stream()
                                .filter(TeacherOptionView::correct)
                                .map(TeacherOptionView::id)
                                .toList()))
                .toList();
        attempts.submit(quiz.id(), new TakeQuizRequest(student, answers));
    }
}
