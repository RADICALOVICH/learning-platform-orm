package ru.mifi.learning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import ru.mifi.learning.domain.enums.EnrollmentStatus;
import ru.mifi.learning.dto.assignment.AssignmentRequest;
import ru.mifi.learning.dto.assignment.SubmitRequest;
import ru.mifi.learning.dto.enrollment.StudentCourseResponse;
import ru.mifi.learning.dto.enrollment.StudentRequest;
import ru.mifi.learning.exception.BusinessRuleException;
import ru.mifi.learning.exception.ConflictException;
import ru.mifi.learning.exception.NotFoundException;
import ru.mifi.learning.repository.LessonProgressRepository;
import ru.mifi.learning.support.AbstractIntegrationTest;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
class EnrollmentServiceIT extends AbstractIntegrationTest {
    private final EnrollmentService service;
    private final CourseService courses;
    private final ProgressService progress;
    private final SubmissionService solutions;
    private final AssignmentService assignments;
    private final LessonProgressRepository marks;

    @Test
    @DisplayName("Запись на опубликованный курс уникальна; архив закрывает новую запись")
    void enroll() {
        var c = data.publishedCourseWithLessons(1);
        var student = new StudentRequest(data.student().getId());
        var created = service.enroll(c.getId(), student);
        assertThat(created.status()).isEqualTo(EnrollmentStatus.ACTIVE);
        assertThatThrownBy(() -> service.enroll(c.getId(), student)).isInstanceOf(ConflictException.class);
        courses.archive(c.getId());
        assertThatThrownBy(() -> service.enroll(
                        c.getId(), new StudentRequest(data.student().getId())))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code")
                .isEqualTo("COURSE_NOT_OPEN");
    }

    @Test
    @DisplayName("В старый опубликованный курс без уроков записаться нельзя")
    void legacyEmptyCourse() {
        var course = data.publishedCourseWithLessons(0);
        var student = new StudentRequest(data.student().getId());
        assertThatThrownBy(() -> service.enroll(course.getId(), student))
                .extracting("code")
                .isEqualTo("COURSE_EMPTY");
        assertThat(service.studentCourses(student.studentId())).isEmpty();
    }

    @Test
    @DisplayName("Списки студентов и курсов соблюдают план загрузки")
    void list() {
        var c = data.publishedCourseWithLessons(1);
        var module = c.getModules().getFirst();
        var lesson = module.getLessons().getFirst();
        var e = data.enrollment(c);
        long student = e.getStudent().getId();
        data.enrollment(c);
        queries.reset();
        assertThat(service.students(c.getId(), PageRequest.of(0, 1)).totalElements())
                .isEqualTo(2);
        queries.assertCount(2);
        queries.reset();
        assertThat(service.studentCourses(student))
                .extracting(StudentCourseResponse::courseId)
                .containsExactly(c.getId());
        queries.assertCount(1);
    }

    @Test
    @DisplayName("Отписка удаляет прогресс и оставляет решение")
    void history() {
        var c = data.publishedCourseWithLessons(1);
        var module = c.getModules().getFirst();
        var lesson = module.getLessons().getFirst();
        var e = data.enrollment(c);
        long student = e.getStudent().getId();
        var a = assignments.create(lesson.getId(), new AssignmentRequest("ДЗ", "Условие", null, null));
        var s = solutions.submit(a.id(), new SubmitRequest(student, "Ответ"));
        progress.mark(lesson.getId(), new StudentRequest(student));
        service.unsubscribe(c.getId(), student);
        assertThat(marks.countByEnrollmentId(e.getId())).isZero();
        assertThat(solutions.get(s.id()).id()).isEqualTo(s.id());
        assertThatThrownBy(() -> service.unsubscribe(c.getId(), student)).isInstanceOf(NotFoundException.class);
    }
}
