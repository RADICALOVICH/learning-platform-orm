package ru.mifi.learning.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import ru.mifi.learning.domain.entity.Notification;
import ru.mifi.learning.domain.entity.Profile;
import ru.mifi.learning.domain.entity.User;
import ru.mifi.learning.domain.enums.NotificationType;
import ru.mifi.learning.domain.enums.Role;
import ru.mifi.learning.support.AbstractRepositoryTest;

class UserRepositoryIT extends AbstractRepositoryTest {
    @Autowired
    UserRepository users;

    @Autowired
    ProfileRepository profiles;

    @Test
    @DisplayName("Поиск находит нормализованный email независимо от регистра")
    void email() {
        User user = users.save(new User("Case@Example.test", "Имя", Role.STUDENT));
        flushAndClear();
        assertThat(users.findByEmailIgnoreCase("CASE@EXAMPLE.TEST")).contains(user);
        assertThat(users.findById(user.getId()).orElseThrow().getEmail()).isEqualTo("case@example.test");
    }

    @Test
    @DisplayName("Уникальность email сохраняется при смене регистра")
    void duplicate() {
        users.saveAndFlush(new User("one@example.test", "Один", Role.STUDENT));
        assertThatThrownBy(() -> users.saveAndFlush(new User("ONE@example.test", "Два", Role.ADMIN)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Профиль получает PK пользователя")
    void sharedKey() {
        User user = data.student();
        Profile profile = profiles.saveAndFlush(new Profile(user));
        flushAndClear();
        assertThat(profile.getId()).isEqualTo(user.getId());
        assertThat(profiles.findById(user.getId()).orElseThrow().getUser()).isEqualTo(user);
    }

    @Test
    @DisplayName("При удалении пользователя PostgreSQL удаляет профиль и уведомления")
    void cascade() {
        User user = data.student();
        profiles.save(new Profile(user));
        Notification notification =
                em.persist(new Notification(user, NotificationType.COURSE_ENROLLED, "Запись", "Курс"));
        flushAndClear();
        users.deleteById(user.getId());
        flushAndClear();
        assertThat(profiles.existsById(user.getId())).isFalse();
        assertThat(em.find(Notification.class, notification.getId())).isNull();
    }

    @Test
    @DisplayName("Стабильный хеш и симметричное равенство работают и с отсоединённым прокси")
    void proxyIdentity() {
        User user = new User("proxy@example.test", "Прокси", Role.STUDENT);
        Set<User> set = new HashSet<>();
        set.add(user);
        users.save(user);
        flushAndClear();
        User loaded = users.findById(user.getId()).orElseThrow();
        em.clear();
        User proxy = users.getReferenceById(user.getId());
        em.clear();
        assertThat(user).isEqualTo(loaded).isEqualTo(proxy);
        assertThat(proxy).isEqualTo(user);
        assertThat(set).contains(proxy);
        assertThat(proxy.toString()).isEqualTo("User#" + user.getId());
        assertThat(Hibernate.isInitialized(proxy)).isFalse();
    }
}
