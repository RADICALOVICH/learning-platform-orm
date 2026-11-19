package ru.mifi.learning.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import ru.mifi.learning.domain.entity.Course;
import ru.mifi.learning.domain.entity.Enrollment;
import ru.mifi.learning.domain.entity.User;
import ru.mifi.learning.domain.enums.CourseStatus;
import ru.mifi.learning.domain.enums.Role;
import ru.mifi.learning.dto.enrollment.StudentRequest;
import ru.mifi.learning.exception.BusinessRuleException;
import ru.mifi.learning.exception.ConflictException;
import ru.mifi.learning.mapper.CourseMapper;
import ru.mifi.learning.mapper.EnrollmentMapper;
import ru.mifi.learning.repository.CourseRepository;
import ru.mifi.learning.repository.EnrollmentRepository;
import ru.mifi.learning.repository.UserRepository;
import ru.mifi.learning.service.event.StudentEnrolledEvent;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {
    @Mock
    EnrollmentRepository enrollments;

    @Mock
    CourseRepository courses;

    @Mock
    UserRepository users;

    @Mock
    Clock clock;

    @Mock
    ApplicationEventPublisher events;

    @Mock
    EnrollmentMapper mapper;

    @Mock
    CourseMapper courseMapper;

    @InjectMocks
    EnrollmentService service;

    private final User student = mock(User.class);
    private final Course course = mock(Course.class);

    @BeforeEach
    void setup() {
        when(courses.lockById(3L)).thenReturn(Optional.of(course));
        when(users.lockById(8L)).thenReturn(Optional.of(student));
    }

    @Test
    @DisplayName("Роль преподавателя не допускает запись как студента")
    void role() {
        when(student.getRole()).thenReturn(Role.TEACHER);
        assertThatThrownBy(() -> service.enroll(3, new StudentRequest(8L)))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code")
                .isEqualTo("USER_NOT_STUDENT");
    }

    @Test
    @DisplayName("Черновик закрыт для записи")
    void draft() {
        when(student.getRole()).thenReturn(Role.STUDENT);
        when(course.getStatus()).thenReturn(CourseStatus.DRAFT);
        assertThatThrownBy(() -> service.enroll(3, new StudentRequest(8L)))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code")
                .isEqualTo("COURSE_NOT_OPEN");
    }

    @Test
    @DisplayName("Повторная запись возвращает конфликт")
    void duplicate() {
        open();
        when(enrollments.existsByStudentIdAndCourseId(8L, 3L)).thenReturn(true);
        assertThatThrownBy(() -> service.enroll(3, new StudentRequest(8L))).isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("После сохранения публикуется идентификатор записи")
    void success() {
        open();
        when(courses.countLessonsByCourseId(3L)).thenReturn(1L);
        var saved = mock(Enrollment.class);
        when(saved.getId()).thenReturn(19L);
        when(enrollments.save(any())).thenReturn(saved);
        service.enroll(3, new StudentRequest(8L));
        verify(events).publishEvent(new StudentEnrolledEvent(8L, 3L, 19L));
    }

    private void open() {
        when(student.getRole()).thenReturn(Role.STUDENT);
        when(student.getId()).thenReturn(8L);
        when(course.getStatus()).thenReturn(CourseStatus.PUBLISHED);
    }
}
