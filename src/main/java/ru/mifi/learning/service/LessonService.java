package ru.mifi.learning.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mifi.learning.domain.OrderIndex;
import ru.mifi.learning.domain.entity.Lesson;
import ru.mifi.learning.dto.course.LessonRequest;
import ru.mifi.learning.dto.course.LessonResponse;
import ru.mifi.learning.exception.ConflictException;
import ru.mifi.learning.exception.NotFoundException;
import ru.mifi.learning.mapper.CourseMapper;
import ru.mifi.learning.repository.CourseModuleRepository;
import ru.mifi.learning.repository.CourseRepository;
import ru.mifi.learning.repository.LessonRepository;
import ru.mifi.learning.repository.SubmissionRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonService {
    private final LessonRepository lessons;
    private final CourseModuleRepository modules;
    private final CourseRepository courses;
    private final CourseCompletionService completion;
    private final SubmissionRepository submissions;
    private final CourseMapper mapper;

    static Lesson newLesson(LessonRequest request) {
        var lesson = new Lesson(request.title());
        apply(lesson, request);
        return lesson;
    }

    private static void apply(Lesson lesson, LessonRequest request) {
        lesson.setTitle(request.title());
        lesson.setContent(request.content());
        lesson.setVideoUrl(request.videoUrl());
        lesson.setDurationMinutes(request.durationMinutes());
        if (request.orderIndex() != null) {
            lesson.setOrderIndex(request.orderIndex());
        }
    }

    @Transactional
    public LessonResponse create(long id, LessonRequest request) {
        var module = modules.lockById(id).orElseThrow(() -> NotFoundException.of("Module", id));
        var lesson = newLesson(request);
        if (lesson.getOrderIndex() == null) {
            lesson.setOrderIndex(
                    OrderIndex.after(lessons.findMaxOrderIndexByModuleId(id).orElse(-1)));
        }
        module.addLesson(lesson);
        lessons.save(lesson);
        return mapper.lesson(lesson);
    }

    public List<LessonResponse> list(long id) {
        return modules.findById(id).orElseThrow(() -> NotFoundException.of("Module", id)).getLessons().stream()
                .map(mapper::lesson)
                .toList();
    }

    public LessonResponse get(long id) {
        return mapper.lesson(require(id));
    }

    @Transactional
    public LessonResponse update(long id, LessonRequest request) {
        var lesson = require(id);
        apply(lesson, request);
        return mapper.lesson(lesson);
    }

    @Transactional
    public void delete(long id) {
        var course = courses.lockByLessonId(id).orElseThrow(() -> NotFoundException.of("Lesson", id));
        var lesson = require(id);
        if (submissions.existsByAssignmentLessonId(id)) {
            throw new ConflictException("LESSON_HAS_SUBMISSIONS", "У заданий урока есть решения");
        }
        lesson.getModule().removeLesson(lesson);
        completion.afterContentRemoval(course);
    }

    private Lesson require(long id) {
        return lessons.findById(id).orElseThrow(() -> NotFoundException.of("Lesson", id));
    }
}
