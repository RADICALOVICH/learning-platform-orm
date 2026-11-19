package ru.mifi.learning.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mifi.learning.domain.entity.CourseModule;
import ru.mifi.learning.dto.course.ModuleRequest;
import ru.mifi.learning.dto.course.ModuleResponse;
import ru.mifi.learning.exception.ConflictException;
import ru.mifi.learning.exception.NotFoundException;
import ru.mifi.learning.mapper.CourseMapper;
import ru.mifi.learning.repository.CourseModuleRepository;
import ru.mifi.learning.repository.CourseRepository;
import ru.mifi.learning.repository.QuizSubmissionRepository;
import ru.mifi.learning.repository.SubmissionRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ModuleService {
    private final CourseRepository courses;
    private final CourseModuleRepository modules;
    private final SubmissionRepository submissions;
    private final QuizSubmissionRepository attempts;
    private final CourseMapper mapper;

    @Transactional
    public ModuleResponse create(long id, ModuleRequest request) {
        var course = courses.lockById(id).orElseThrow(() -> NotFoundException.of("Course", id));
        var module = new CourseModule(request.title());
        module.setOrderIndex(
                request.orderIndex() == null
                        ? modules.findMaxOrderIndexByCourseId(id).orElse(-1) + 1
                        : request.orderIndex());
        module.setDescription(request.description());
        course.addModule(module);
        modules.save(module);
        return mapper.module(module);
    }

    public List<ModuleResponse> list(long id) {
        var rows = modules.findWithLessonsByCourseId(id);
        if (rows.isEmpty() && !courses.existsById(id)) {
            throw NotFoundException.of("Course", id);
        }
        return rows.stream().map(mapper::module).toList();
    }

    public ModuleResponse get(long id) {
        return mapper.module(require(id));
    }

    @Transactional
    public ModuleResponse update(long id, ModuleRequest request) {
        var module = require(id);
        module.setTitle(request.title());
        module.setDescription(request.description());
        if (request.orderIndex() != null) {
            module.setOrderIndex(request.orderIndex());
        }
        return mapper.module(module);
    }

    @Transactional
    public void delete(long id) {
        var module = require(id);
        if (submissions.existsByAssignmentLessonModuleId(id) || attempts.existsByQuizModuleId(id)) {
            throw new ConflictException("MODULE_HAS_LEARNING_DATA", "Модуль содержит результаты студентов");
        }
        module.getCourse().removeModule(module);
    }

    private CourseModule require(long id) {
        return modules.findById(id).orElseThrow(() -> NotFoundException.of("Module", id));
    }
}
