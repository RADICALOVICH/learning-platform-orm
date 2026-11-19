package ru.mifi.learning.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import ru.mifi.learning.domain.entity.AnswerOption;
import ru.mifi.learning.domain.entity.Assignment;
import ru.mifi.learning.domain.entity.Course;
import ru.mifi.learning.domain.entity.CourseModule;
import ru.mifi.learning.domain.entity.Lesson;
import ru.mifi.learning.domain.entity.Question;
import ru.mifi.learning.domain.entity.Quiz;
import ru.mifi.learning.domain.entity.Tag;
import ru.mifi.learning.domain.enums.CourseStatus;
import ru.mifi.learning.repository.spec.CourseSpecifications;
import ru.mifi.learning.support.AbstractRepositoryTest;

class CourseRepositoryIT extends AbstractRepositoryTest {
    @Autowired
    CourseRepository courses;

    @Autowired
    CourseModuleRepository modules;

    @Autowired
    QuizRepository quizzes;

    @Test
    @DisplayName("Дерево от курса до задания сохраняется одним save")
    void cascade() {
        Course course = data.course();
        CourseModule module = new CourseModule("Модуль");
        Lesson lesson = new Lesson("Урок");
        Assignment assignment = new Assignment("Практика", "Условие");
        course.addModule(module);
        module.addLesson(lesson);
        lesson.addAssignment(assignment);
        courses.save(course);
        flushAndClear();
        assertThat(em.find(Assignment.class, assignment.getId())
                        .getLesson()
                        .getModule()
                        .getCourse())
                .isEqualTo(course);
    }

    @Test
    @DisplayName("Удаление модуля из коллекции удаляет его уроки")
    void orphan() {
        Course course = data.publishedCourseWithLessons(1);
        CourseModule module = course.getModules().getFirst();
        Lesson lesson = module.getLessons().getFirst();
        flushAndClear();
        Course loaded = courses.findById(course.getId()).orElseThrow();
        loaded.removeModule(loaded.getModules().getFirst());
        flushAndClear();
        assertThat(em.find(CourseModule.class, module.getId())).isNull();
        assertThat(em.find(Lesson.class, lesson.getId())).isNull();
    }

    @Test
    @DisplayName("Модули и уроки в заданном порядке загружаются двумя запросами")
    void structure() {
        Course course = data.course();
        CourseModule last = new CourseModule("Последний");
        last.setOrderIndex(3);
        course.addModule(last);
        CourseModule first = new CourseModule("Первый");
        first.setOrderIndex(1);
        course.addModule(first);
        Lesson second = new Lesson("Б");
        second.setOrderIndex(4);
        first.addLesson(second);
        Lesson initial = new Lesson("А");
        initial.setOrderIndex(0);
        first.addLesson(initial);
        courses.save(course);
        flushAndClear();
        queries.reset();
        Course loaded = courses.findWithModulesById(course.getId()).orElseThrow();
        modules.findWithLessonsByCourseId(course.getId());
        assertThat(loaded.getModules()).extracting(CourseModule::getTitle).containsExactly("Первый", "Последний");
        assertThat(loaded.getModules().getFirst().getLessons())
                .extracting(Lesson::getTitle)
                .containsExactly("А", "Б");
        assertThat(loaded.getModules().getLast().getLessons()).isEmpty();
        queries.assertCount(2);
    }

    @Test
    @DisplayName("Каталог сочетает фильтры и пагинацию без дублирования курсов")
    void filter() {
        Course target = data.publishedCourseWithLessons(1);
        target.setTitle("Курс 100% ORM");
        Tag tag = em.persist(new Tag("ORM"));
        target.addTag(tag);
        Course other = new Course("Курс 100% ORM", target.getCategory(), target.getTeacher());
        courses.save(other);
        Course similar = new Course("Курс 100X ORM", target.getCategory(), target.getTeacher());
        similar.publish();
        similar.addTag(tag);
        courses.save(similar);
        flushAndClear();
        var spec = CourseSpecifications.search(
                target.getCategory().getId(), target.getTeacher().getId(), "OrM", CourseStatus.PUBLISHED, "100% orm");
        var page = courses.findAll(spec, PageRequest.of(0, 1));
        assertThat(page.getTotalElements()).isOne();
        assertThat(page.getContent()).containsExactly(target);
        assertThat(Hibernate.isInitialized(page.getContent().getFirst().getTeacher()))
                .isTrue();
        assertThat(courses.findAll(CourseSpecifications.search(null, null, "другой", null, null), PageRequest.of(0, 1)))
                .isEmpty();
    }

    @Test
    @DisplayName("Удаление одного тега оставляет остальные строки связи")
    void joinRows() {
        Course course = data.course();
        Tag a = em.persist(new Tag("a"));
        Tag b = em.persist(new Tag("b"));
        course.addTag(a);
        course.addTag(b);
        courses.save(course);
        flushAndClear();
        Course loaded = courses.findDetailsById(course.getId()).orElseThrow();
        loaded.removeTag(em.find(Tag.class, a.getId()));
        flushAndClear();
        assertThat(courses.findDetailsById(course.getId()).orElseThrow().getTags())
                .containsExactly(b);
        assertThat(em.find(Tag.class, a.getId())).isNotNull();
    }

    @Test
    @DisplayName("При удалении курса БД удаляет тест модуля, вопросы и варианты")
    void cascadeQuiz() {
        Course course = data.publishedCourseWithLessons(1);
        Quiz quiz = data.quizWithQuestions(course.getModules().getFirst(), 1);
        Question question = quiz.getQuestions().getFirst();
        AnswerOption option = question.getOptions().getFirst();
        flushAndClear();
        courses.deleteById(course.getId());
        flushAndClear();
        assertThat(quizzes.existsById(quiz.getId())).isFalse();
        assertThat(em.find(Question.class, question.getId())).isNull();
        assertThat(em.find(AnswerOption.class, option.getId())).isNull();
    }

    @Test
    @DisplayName("Внешний ключ защищает курс с записью студента от удаления")
    void protectHistory() {
        Course course = data.publishedCourseWithLessons(1);
        data.enrollment(course);
        flushAndClear();
        courses.deleteById(course.getId());
        assertThatThrownBy(courses::flush).isInstanceOf(DataIntegrityViolationException.class);
    }
}
