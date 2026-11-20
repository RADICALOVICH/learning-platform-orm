package ru.mifi.learning.service;

import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mifi.learning.domain.entity.Category;
import ru.mifi.learning.domain.entity.Course;
import ru.mifi.learning.domain.entity.CourseModule;
import ru.mifi.learning.domain.entity.Tag;
import ru.mifi.learning.domain.entity.User;
import ru.mifi.learning.domain.enums.CourseStatus;
import ru.mifi.learning.dto.common.PageResponse;
import ru.mifi.learning.dto.course.CourseDetailsResponse;
import ru.mifi.learning.dto.course.CourseStructureResponse;
import ru.mifi.learning.dto.course.CourseSummaryResponse;
import ru.mifi.learning.dto.course.CourseTagsRequest;
import ru.mifi.learning.dto.course.CreateCourseRequest;
import ru.mifi.learning.dto.course.UpdateCourseRequest;
import ru.mifi.learning.exception.BusinessRuleException;
import ru.mifi.learning.exception.ConflictException;
import ru.mifi.learning.exception.NotFoundException;
import ru.mifi.learning.mapper.CourseMapper;
import ru.mifi.learning.repository.CategoryRepository;
import ru.mifi.learning.repository.CourseModuleRepository;
import ru.mifi.learning.repository.CourseRepository;
import ru.mifi.learning.repository.CourseReviewRepository;
import ru.mifi.learning.repository.EnrollmentRepository;
import ru.mifi.learning.repository.PageRequestValidator;
import ru.mifi.learning.repository.QuizRepository;
import ru.mifi.learning.repository.QuizSubmissionRepository;
import ru.mifi.learning.repository.SubmissionRepository;
import ru.mifi.learning.repository.TagRepository;
import ru.mifi.learning.repository.UserRepository;
import ru.mifi.learning.repository.projection.ModuleQuizId;
import ru.mifi.learning.repository.spec.CourseSpecifications;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {
    private final CourseRepository courses;
    private final PageRequestValidator pageRequests;
    private final UserRepository users;
    private final CategoryRepository categories;
    private final TagRepository tags;
    private final CourseModuleRepository modules;
    private final QuizRepository quizzes;
    private final EnrollmentRepository enrollments;
    private final SubmissionRepository submissions;
    private final QuizSubmissionRepository attempts;
    private final CourseReviewRepository reviews;
    private final CourseMapper mapper;

    @Transactional
    public CourseDetailsResponse create(CreateCourseRequest request) {
        var course = new Course(request.title(), category(request.categoryId()), teacher(request.teacherId()));
        course.setDescription(request.description());
        course.setStartDate(request.startDate());
        course.setDurationWeeks(request.durationWeeks());
        if (request.tags() != null) {
            replaceTags(course, request.tags());
        }
        if (request.modules() != null) {
            for (var input : request.modules()) {
                var module = new CourseModule(input.title());
                module.setDescription(input.description());
                module.setOrderIndex(input.orderIndex());
                course.addModule(module);
                if (input.lessons() != null) {
                    for (var lesson : input.lessons()) {
                        module.addLesson(LessonService.newLesson(lesson));
                    }
                }
            }
        }
        courses.save(course);
        log.info("Создан курс id={}", course.getId());
        return get(course.getId());
    }

    public CourseDetailsResponse get(long id) {
        var rows = courses.findCardRows(id);
        if (rows.isEmpty()) {
            throw NotFoundException.of("Course", id);
        }
        return mapper.details(rows);
    }

    public PageResponse<CourseSummaryResponse> list(
            Long categoryId, Long teacherId, String tag, CourseStatus status, String q, Pageable pageable) {
        pageRequests.validate(Course.class, pageable);
        return PageResponse.from(
                courses.findAll(CourseSpecifications.search(categoryId, teacherId, tag, status, q), pageable)
                        .map(mapper::summary));
    }

    public CourseStructureResponse structure(long id) {
        var course = courses.findWithModulesById(id).orElseThrow(() -> NotFoundException.of("Course", id));
        modules.findWithLessonsByCourseId(id);
        return mapper.structure(
                course,
                quizzes.findModuleQuizIdsByCourseId(id).stream()
                        .collect(Collectors.toMap(ModuleQuizId::moduleId, ModuleQuizId::quizId)));
    }

    @Transactional
    public CourseDetailsResponse update(long id, UpdateCourseRequest request) {
        var course = locked(id);
        course.changeTeacher(teacher(request.teacherId()));
        course.changeCategory(category(request.categoryId()));
        course.setTitle(request.title());
        course.setDescription(request.description());
        course.setStartDate(request.startDate());
        course.setDurationWeeks(request.durationWeeks());
        return get(id);
    }

    @Transactional
    public CourseDetailsResponse tags(long id, CourseTagsRequest request) {
        replaceTags(locked(id), request.names());
        return get(id);
    }

    @Transactional
    public CourseDetailsResponse publish(long id) {
        var course = locked(id);
        course.publish();
        if (courses.countLessonsByCourseId(id) == 0) {
            throw new BusinessRuleException("COURSE_EMPTY", "Для публикации добавьте урок в модуль курса");
        }
        return get(id);
    }

    @Transactional
    public CourseDetailsResponse archive(long id) {
        locked(id).archive();
        return get(id);
    }

    @Transactional
    public void delete(long id) {
        var course = locked(id);
        if (enrollments.existsByCourseId(id)
                || reviews.existsByCourseId(id)
                || submissions.existsByAssignmentLessonModuleCourseId(id)
                || attempts.existsByQuizModuleCourseId(id)) {
            throw new ConflictException(
                    "COURSE_HAS_LEARNING_DATA", "У курса есть учебная история. Курс можно архивировать");
        }
        courses.delete(course);
    }

    private Course locked(long id) {
        return courses.lockById(id).orElseThrow(() -> NotFoundException.of("Course", id));
    }

    private User teacher(long id) {
        var user = users.lockById(id).orElseThrow(() -> NotFoundException.of("User", id));
        if (!user.getRole().canTeach()) {
            throw new BusinessRuleException("USER_NOT_TEACHER", "Курс может вести преподаватель или администратор");
        }
        return user;
    }

    private Category category(long id) {
        return categories.findById(id).orElseThrow(() -> NotFoundException.of("Category", id));
    }

    private void replaceTags(Course course, List<String> names) {
        var wanted = names.stream().map(Tag::normalizeName).collect(Collectors.toCollection(TreeSet::new));
        for (var tag : List.copyOf(course.getTags())) {
            if (!wanted.remove(tag.getName())) {
                course.removeTag(tag);
            }
        }
        if (wanted.isEmpty()) {
            return;
        }
        for (var existing : tags.findByNameIn(wanted)) {
            course.addTag(existing);
            wanted.remove(existing.getName());
        }
        if (!wanted.isEmpty()) {
            wanted.forEach(tags::insertIfMissing);
            tags.findByNameIn(wanted).forEach(course::addTag);
        }
    }
}
