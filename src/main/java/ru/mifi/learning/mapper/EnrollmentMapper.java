package ru.mifi.learning.mapper;

import org.springframework.stereotype.Component;
import ru.mifi.learning.domain.entity.Enrollment;
import ru.mifi.learning.dto.enrollment.EnrolledStudentResponse;
import ru.mifi.learning.dto.enrollment.EnrollmentResponse;
import ru.mifi.learning.dto.enrollment.StudentCourseResponse;

@Component
public class EnrollmentMapper {
    public EnrollmentResponse enrollment(Enrollment enrollment) {
        return new EnrollmentResponse(
                enrollment.getId(),
                enrollment.getStudent().getId(),
                enrollment.getCourse().getId(),
                enrollment.getStatus(),
                enrollment.getEnrolledAt(),
                enrollment.getCompletedAt());
    }

    public EnrolledStudentResponse student(Enrollment enrollment) {
        var student = enrollment.getStudent();
        return new EnrolledStudentResponse(
                student.getId(),
                student.getFullName(),
                student.getEmail(),
                enrollment.getStatus(),
                enrollment.getEnrolledAt());
    }

    public StudentCourseResponse course(Enrollment enrollment) {
        var course = enrollment.getCourse();
        return new StudentCourseResponse(
                course.getId(),
                course.getTitle(),
                course.getTeacher().getId(),
                course.getTeacher().getFullName(),
                enrollment.getStatus(),
                enrollment.getEnrolledAt());
    }
}
