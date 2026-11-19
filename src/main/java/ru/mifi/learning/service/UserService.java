package ru.mifi.learning.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mifi.learning.domain.entity.User;
import ru.mifi.learning.domain.enums.Role;
import ru.mifi.learning.dto.common.PageResponse;
import ru.mifi.learning.dto.user.CreateUserRequest;
import ru.mifi.learning.dto.user.UpdateUserRequest;
import ru.mifi.learning.dto.user.UserResponse;
import ru.mifi.learning.exception.ConflictException;
import ru.mifi.learning.exception.NotFoundException;
import ru.mifi.learning.mapper.UserMapper;
import ru.mifi.learning.repository.CourseRepository;
import ru.mifi.learning.repository.CourseReviewRepository;
import ru.mifi.learning.repository.EnrollmentRepository;
import ru.mifi.learning.repository.QuizSubmissionRepository;
import ru.mifi.learning.repository.SubmissionRepository;
import ru.mifi.learning.repository.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository users;
    private final CourseRepository courses;
    private final EnrollmentRepository enrollments;
    private final SubmissionRepository submissions;
    private final QuizSubmissionRepository attempts;
    private final CourseReviewRepository reviews;
    private final UserMapper mapper;

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        ensureEmailAvailable(request.email());
        User user = users.save(new User(request.email(), request.fullName().strip(), request.role()));
        log.info("Создан пользователь id={}", user.getId());
        return mapper.toResponse(user);
    }

    public UserResponse get(long id) {
        return mapper.toResponse(requireUser(id));
    }

    public PageResponse<UserResponse> list(Role role, Pageable pageable) {
        Page<User> page = role == null ? users.findAll(pageable) : users.findAllByRole(role, pageable);
        return PageResponse.from(page.map(mapper::toResponse));
    }

    @Transactional
    public UserResponse update(long id, UpdateUserRequest request) {
        User user = users.lockById(id).orElseThrow(() -> NotFoundException.of("User", id));
        if (user.getRole() == Role.STUDENT && request.role() != Role.STUDENT && enrollments.existsByStudentId(id)) {
            throw new ConflictException("USER_HAS_DEPENDENCIES", "Сначала отпишите студента от курсов");
        }
        if (!user.getEmail().equalsIgnoreCase(request.email().strip())) {
            ensureEmailAvailable(request.email());
        }
        if (user.getRole() != request.role()
                && (user.getRole() == Role.TEACHER || request.role() == Role.STUDENT)
                && courses.existsByTeacherId(id)) {
            throw new ConflictException("USER_HAS_DEPENDENCIES", "Сначала передайте курсы другому преподавателю");
        }
        user.setEmail(request.email());
        user.setFullName(request.fullName().strip());
        user.setRole(request.role());
        return mapper.toResponse(user);
    }

    @Transactional
    public void delete(long id) {
        User user = requireUser(id);
        if (courses.existsByTeacherId(id)
                || enrollments.existsByStudentId(id)
                || submissions.existsByStudentId(id)
                || attempts.existsByStudentId(id)
                || reviews.existsByStudentId(id)) {
            throw new ConflictException("USER_HAS_DEPENDENCIES", "У пользователя есть курсы или история обучения");
        }
        users.delete(user);
        log.info("Удалён пользователь id={}", id);
    }

    private User requireUser(long id) {
        return users.findById(id).orElseThrow(() -> NotFoundException.of("User", id));
    }

    private void ensureEmailAvailable(String email) {
        if (users.existsByEmailIgnoreCase(email.strip())) {
            throw new ConflictException("USER_EMAIL_TAKEN", "Этот email уже используется");
        }
    }
}
