package ru.mifi.learning.service;

import java.time.Clock;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mifi.learning.domain.entity.Submission;
import ru.mifi.learning.dto.assignment.GradeRequest;
import ru.mifi.learning.dto.assignment.StudentSubmissionResponse;
import ru.mifi.learning.dto.assignment.SubmissionResponse;
import ru.mifi.learning.dto.assignment.SubmitRequest;
import ru.mifi.learning.exception.BusinessRuleException;
import ru.mifi.learning.exception.ConflictException;
import ru.mifi.learning.exception.NotFoundException;
import ru.mifi.learning.mapper.AssignmentMapper;
import ru.mifi.learning.repository.AssignmentRepository;
import ru.mifi.learning.repository.EnrollmentRepository;
import ru.mifi.learning.repository.SubmissionRepository;
import ru.mifi.learning.repository.UserRepository;
import ru.mifi.learning.service.event.SubmissionGradedEvent;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubmissionService {
    private final AssignmentRepository assignments;
    private final SubmissionRepository submissions;
    private final EnrollmentRepository enrollments;
    private final UserRepository users;
    private final AssignmentMapper mapper;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    @Transactional
    public SubmissionResponse submit(long id, SubmitRequest request) {
        var course = assignments.findCourseIdById(id).orElseThrow(() -> NotFoundException.of("Assignment", id));
        var enrollment = enrollments
                .lockByStudentAndCourse(request.studentId(), course)
                .orElseThrow(() -> new BusinessRuleException("NOT_ENROLLED", "Для сдачи нужна запись на курс"));
        if (submissions.existsByAssignmentIdAndStudentId(id, request.studentId())) {
            throw new ConflictException("SUBMISSION_EXISTS", "Решение уже отправлено");
        }
        return mapper.submission(submissions.save(new Submission(
                assignments.getReferenceById(id), enrollment.getStudent(), request.content(), clock.instant())));
    }

    public SubmissionResponse get(long id) {
        return mapper.submission(require(id));
    }

    public List<SubmissionResponse> list(long id) {
        var rows = submissions.findByAssignmentIdWithStudent(id);
        if (rows.isEmpty() && !assignments.existsById(id)) {
            throw NotFoundException.of("Assignment", id);
        }
        return rows.stream().map(mapper::submission).toList();
    }

    public List<StudentSubmissionResponse> studentSubmissions(long id) {
        var rows = submissions.findByStudentIdWithAssignment(id);
        if (rows.isEmpty() && !users.existsById(id)) {
            throw NotFoundException.of("User", id);
        }
        return rows.stream().map(mapper::studentSubmission).toList();
    }

    @Transactional
    public SubmissionResponse grade(long id, GradeRequest request) {
        var assignment = assignments.lockBySubmissionId(id).orElseThrow(() -> NotFoundException.of("Submission", id));
        var submission = require(id);
        if (submission.grade(request.score(), request.feedback(), clock.instant())) {
            log.info("Оценено решение id={}", id);
            events.publishEvent(
                    new SubmissionGradedEvent(id, submission.getStudent().getId(), assignment.getId()));
        }
        return mapper.submission(submission);
    }

    @Transactional
    public void delete(long id) {
        submissions.delete(require(id));
    }

    private Submission require(long id) {
        return submissions.findById(id).orElseThrow(() -> NotFoundException.of("Submission", id));
    }
}
