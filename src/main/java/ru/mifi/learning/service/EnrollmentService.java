package ru.mifi.learning.service;

import java.time.Clock;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mifi.learning.domain.entity.Enrollment;
import ru.mifi.learning.domain.enums.CourseStatus;
import ru.mifi.learning.domain.enums.Role;
import ru.mifi.learning.dto.common.PageResponse;
import ru.mifi.learning.dto.course.CourseSummaryResponse;
import ru.mifi.learning.dto.enrollment.EnrolledStudentResponse;
import ru.mifi.learning.dto.enrollment.EnrollmentResponse;
import ru.mifi.learning.dto.enrollment.StudentCourseResponse;
import ru.mifi.learning.dto.enrollment.StudentRequest;
import ru.mifi.learning.exception.BusinessRuleException;
import ru.mifi.learning.exception.ConflictException;
import ru.mifi.learning.exception.NotFoundException;
import ru.mifi.learning.mapper.CourseMapper;
import ru.mifi.learning.mapper.EnrollmentMapper;
import ru.mifi.learning.repository.CourseRepository;
import ru.mifi.learning.repository.EnrollmentRepository;
import ru.mifi.learning.repository.PageRequestValidator;
import ru.mifi.learning.repository.UserRepository;
import ru.mifi.learning.service.event.StudentEnrolledEvent;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnrollmentService {
    private final EnrollmentRepository enrollments;
    private final PageRequestValidator pageRequests;
    private final CourseRepository courses;
    private final UserRepository users;
    private final Clock clock;
    private final ApplicationEventPublisher events;
    private final EnrollmentMapper mapper;
    private final CourseMapper courseMapper;

    @Transactional
    public EnrollmentResponse enroll(long id, StudentRequest request) {
        var course = courses.lockById(id).orElseThrow(() -> NotFoundException.of("Course", id));
        var student = users.lockById(request.studentId())
                .orElseThrow(() -> NotFoundException.of("User", request.studentId()));
        if (student.getRole() != Role.STUDENT) {
            throw new BusinessRuleException("USER_NOT_STUDENT", "На курс можно записать только студента");
        }
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new BusinessRuleException("COURSE_NOT_OPEN", "Курс закрыт для записи");
        }
        if (enrollments.existsByStudentIdAndCourseId(student.getId(), id)) {
            throw new ConflictException("ALREADY_ENROLLED", "Студент уже записан на курс");
        }
        if (courses.countLessonsByCourseId(id) == 0) {
            throw new BusinessRuleException("COURSE_EMPTY", "Для записи в курсе нужен хотя бы один урок");
        }
        var enrollment = enrollments.save(new Enrollment(student, course, clock.instant()));
        log.info("Запись на курс courseId={}, enrollmentId={}", id, enrollment.getId());
        events.publishEvent(new StudentEnrolledEvent(student.getId(), id, enrollment.getId()));
        return mapper.enrollment(enrollment);
    }

    @Transactional
    public void unsubscribe(long id, long studentId) {
        var enrollment = enrollments
                .lockByStudentAndCourse(studentId, id)
                .orElseThrow(() -> NotFoundException.enrollment(id, studentId));
        enrollments.delete(enrollment);
    }

    public EnrollmentResponse get(long courseId, long studentId) {
        return mapper.enrollment(enrollments
                .findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> NotFoundException.enrollment(courseId, studentId)));
    }

    public PageResponse<EnrolledStudentResponse> students(long id, Pageable pageable) {
        pageRequests.validate(Enrollment.class, pageable);
        var page = enrollments.findWithStudentByCourseId(id, pageable);
        if (page.isEmpty() && !courses.existsById(id)) {
            throw NotFoundException.of("Course", id);
        }
        return PageResponse.from(page.map(mapper::student));
    }

    public List<StudentCourseResponse> studentCourses(long id) {
        var rows = enrollments.findByStudentIdWithCourse(id);
        if (rows.isEmpty()) {
            requireUser(id);
        }
        return rows.stream().map(mapper::course).toList();
    }

    public List<CourseSummaryResponse> taught(long id) {
        var rows = courses.findByTeacherIdOrderById(id);
        if (rows.isEmpty()) {
            requireUser(id);
        }
        return rows.stream().map(courseMapper::summary).toList();
    }

    private void requireUser(long id) {
        if (!users.existsById(id)) {
            throw NotFoundException.of("User", id);
        }
    }
}
