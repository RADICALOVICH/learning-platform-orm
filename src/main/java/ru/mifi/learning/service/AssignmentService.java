package ru.mifi.learning.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mifi.learning.domain.entity.Assignment;
import ru.mifi.learning.dto.assignment.AssignmentRequest;
import ru.mifi.learning.dto.assignment.AssignmentResponse;
import ru.mifi.learning.exception.ConflictException;
import ru.mifi.learning.exception.NotFoundException;
import ru.mifi.learning.mapper.AssignmentMapper;
import ru.mifi.learning.repository.AssignmentRepository;
import ru.mifi.learning.repository.LessonRepository;
import ru.mifi.learning.repository.SubmissionRepository;
import ru.mifi.learning.repository.UserRepository;
import ru.mifi.learning.repository.projection.StudentAssignment;
import ru.mifi.learning.service.event.AssignmentCreatedEvent;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssignmentService {
    private final AssignmentRepository assignments;
    private final LessonRepository lessons;
    private final SubmissionRepository submissions;
    private final UserRepository users;
    private final AssignmentMapper mapper;
    private final ApplicationEventPublisher events;

    @Transactional
    public AssignmentResponse create(long id, AssignmentRequest request) {
        var courseId = lessons.findCourseIdById(id).orElseThrow(() -> NotFoundException.of("Lesson", id));
        var assignment = new Assignment(request.title(), request.description());
        apply(assignment, request);
        lessons.getReferenceById(id).addAssignment(assignment);
        assignments.save(assignment);
        events.publishEvent(new AssignmentCreatedEvent(assignment.getId(), courseId));
        return mapper.assignment(assignment);
    }

    public List<AssignmentResponse> list(long id) {
        var rows = assignments.findByLessonId(id);
        if (rows.isEmpty() && !lessons.existsById(id)) {
            throw NotFoundException.of("Lesson", id);
        }
        return rows.stream().map(mapper::assignment).toList();
    }

    public AssignmentResponse get(long id) {
        return mapper.assignment(require(id));
    }

    @Transactional
    public AssignmentResponse update(long id, AssignmentRequest request) {
        var assignment = require(id);
        apply(assignment, request);
        return mapper.assignment(assignment);
    }

    @Transactional
    public void delete(long id) {
        var assignment = require(id);
        if (submissions.existsByAssignmentId(id)) {
            throw new ConflictException("ASSIGNMENT_HAS_SUBMISSIONS", "По заданию уже есть решения");
        }
        assignment.getLesson().removeAssignment(assignment);
    }

    public List<StudentAssignment> studentAssignments(long id) {
        var rows = assignments.findStudentAssignments(id);
        if (rows.isEmpty() && !users.existsById(id)) {
            throw NotFoundException.of("User", id);
        }
        return rows;
    }

    private Assignment require(long id) {
        return assignments.findById(id).orElseThrow(() -> NotFoundException.of("Assignment", id));
    }

    private static void apply(Assignment assignment, AssignmentRequest request) {
        assignment.setTitle(request.title());
        assignment.setDescription(request.description());
        assignment.setDueDate(request.dueDate());
        assignment.setMaxScore(request.maxScore() == null ? 100 : request.maxScore());
    }
}
