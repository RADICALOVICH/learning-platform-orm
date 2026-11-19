package ru.mifi.learning.mapper;

import org.springframework.stereotype.Component;
import ru.mifi.learning.domain.entity.Assignment;
import ru.mifi.learning.domain.entity.Submission;
import ru.mifi.learning.dto.assignment.AssignmentResponse;
import ru.mifi.learning.dto.assignment.StudentSubmissionResponse;
import ru.mifi.learning.dto.assignment.SubmissionResponse;

@Component
public class AssignmentMapper {
    public AssignmentResponse assignment(Assignment assignment) {
        return new AssignmentResponse(
                assignment.getId(),
                assignment.getLesson().getId(),
                assignment.getTitle(),
                assignment.getDescription(),
                assignment.getDueDate(),
                assignment.getMaxScore());
    }

    public SubmissionResponse submission(Submission submission) {
        return new SubmissionResponse(
                submission.getId(),
                submission.getAssignment().getId(),
                submission.getStudent().getId(),
                submission.getStudent().getFullName(),
                submission.getContent(),
                submission.getSubmittedAt(),
                submission.isLate(),
                submission.getStatus(),
                submission.getScore(),
                submission.getFeedback(),
                submission.getGradedAt());
    }

    public StudentSubmissionResponse studentSubmission(Submission submission) {
        var assignment = submission.getAssignment();
        var course = assignment.getLesson().getModule().getCourse();
        return new StudentSubmissionResponse(
                submission.getId(),
                assignment.getId(),
                assignment.getTitle(),
                course.getId(),
                course.getTitle(),
                submission.getScore(),
                submission.getStatus());
    }
}
