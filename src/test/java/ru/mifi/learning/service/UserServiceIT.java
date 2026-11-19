package ru.mifi.learning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import ru.mifi.learning.domain.entity.Course;
import ru.mifi.learning.domain.enums.Role;
import ru.mifi.learning.dto.user.CreateUserRequest;
import ru.mifi.learning.dto.user.UpdateUserRequest;
import ru.mifi.learning.dto.user.UpsertProfileRequest;
import ru.mifi.learning.exception.ConflictException;
import ru.mifi.learning.exception.NotFoundException;
import ru.mifi.learning.repository.ProfileRepository;
import ru.mifi.learning.repository.UserRepository;
import ru.mifi.learning.support.AbstractIntegrationTest;

class UserServiceIT extends AbstractIntegrationTest {
    @Autowired
    UserService users;

    @Autowired
    ProfileService profiles;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ProfileRepository profileRepository;

    @Test
    @DisplayName("Создание, фильтр по роли и обновление выполняются в отдельных транзакциях")
    void createListUpdate() {
        var student = users.create(new CreateUserRequest("Student@example.test", "Студент", Role.STUDENT));
        data.teacher();
        assertThat(student.email()).isEqualTo("student@example.test");
        var page = users.list(Role.STUDENT, PageRequest.of(0, 1));
        assertThat(page.content()).containsExactly(student);
        assertThat(page.totalElements()).isOne();
        var updated =
                users.update(student.id(), new UpdateUserRequest("STUDENT@example.test", "Администратор", Role.ADMIN));
        assertThat(users.get(student.id())).isEqualTo(updated);
        assertThat(updated.fullName()).isEqualTo("Администратор");
        assertThat(updated.role()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("При конфликте email создание и изменение не затрагивают существующих пользователей")
    void duplicateEmail() {
        users.create(new CreateUserRequest("first@example.test", "Первый", Role.STUDENT));
        var other = users.create(new CreateUserRequest("other@example.test", "Другой", Role.TEACHER));
        assertThatThrownBy(() -> users.create(new CreateUserRequest("FIRST@example.test", "Повтор", Role.STUDENT)))
                .isInstanceOf(ConflictException.class)
                .extracting("code")
                .isEqualTo("USER_EMAIL_TAKEN");
        assertThatThrownBy(() ->
                        users.update(other.id(), new UpdateUserRequest("First@example.test", "Изменение", Role.ADMIN)))
                .isInstanceOf(ConflictException.class);
        assertThat(users.get(other.id())).isEqualTo(other);
    }

    @Test
    @DisplayName("Преподаватель с курсом сохраняет роль и учётную запись")
    void protectsTeacher() {
        Course course = data.publishedCourseWithLessons(1);
        long id = course.getTeacher().getId();
        assertThatThrownBy(() -> users.delete(id)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> users.update(id, new UpdateUserRequest("t@example.test", "Имя", Role.STUDENT)))
                .isInstanceOf(ConflictException.class)
                .extracting("code")
                .isEqualTo("USER_HAS_DEPENDENCIES");
        assertThat(users.get(id).role()).isEqualTo(Role.TEACHER);
    }

    @Test
    @DisplayName("Студента с записью на курс нельзя удалить")
    void protectsStudent() {
        var enrollment = data.enrollment(data.publishedCourseWithLessons(1));
        assertThatThrownBy(() -> users.delete(enrollment.getStudent().getId())).isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("PUT создаёт один профиль и заменяет его поля при повторном запросе")
    void upsertProfile() {
        long id = data.student().getId();
        assertThatThrownBy(() -> profiles.get(id))
                .isInstanceOf(NotFoundException.class)
                .extracting("code")
                .isEqualTo("PROFILE_NOT_FOUND");
        var before = profiles.upsert(
                id, new UpsertProfileRequest("Биография", null, "123", "Москва", LocalDate.of(2000, 1, 1)));
        assertThat(before.userId()).isEqualTo(id);
        assertThat(profiles.get(id)).isEqualTo(before);
        var update = new UpsertProfileRequest("Правка", null, null, null, null);
        var after = profiles.upsert(id, update);
        assertThat(profiles.upsert(id, update)).isEqualTo(after);
        assertThat(profiles.get(id).city()).isNull();
        assertThat(profileRepository.count()).isOne();
        users.delete(id);
        assertThat(profileRepository.existsById(id)).isFalse();
        assertThat(userRepository.existsById(id)).isFalse();
        assertThatThrownBy(() -> profiles.upsert(id, update))
                .isInstanceOf(NotFoundException.class)
                .extracting("code")
                .isEqualTo("USER_NOT_FOUND");
    }

    @Test
    @DisplayName("Два одновременных PUT нового профиля получают один и тот же ID")
    void concurrentUpsert() throws Exception {
        long id = data.student().getId();
        var request = new UpsertProfileRequest("Биография", null, null, null, null);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> {
                start.await();
                return profiles.upsert(id, request);
            });
            var second = executor.submit(() -> {
                start.await();
                return profiles.upsert(id, request);
            });
            start.countDown();
            assertThat(first.get()).isEqualTo(second.get());
            assertThat(profileRepository.count()).isOne();
        }
    }
}
