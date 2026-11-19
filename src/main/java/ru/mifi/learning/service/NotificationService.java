package ru.mifi.learning.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mifi.learning.domain.entity.Notification;
import ru.mifi.learning.dto.common.PageResponse;
import ru.mifi.learning.dto.notification.NotificationResponse;
import ru.mifi.learning.dto.notification.ReadAllResponse;
import ru.mifi.learning.exception.NotFoundException;
import ru.mifi.learning.mapper.NotificationMapper;
import ru.mifi.learning.repository.NotificationRepository;
import ru.mifi.learning.repository.PageRequestValidator;
import ru.mifi.learning.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {
    private final NotificationRepository notifications;
    private final PageRequestValidator pageRequests;
    private final UserRepository users;
    private final NotificationMapper mapper;

    public PageResponse<NotificationResponse> list(long userId, boolean unreadOnly, Pageable pageable) {
        pageRequests.validate(Notification.class, pageable);
        var page = notifications.findByRecipientId(userId, unreadOnly, pageable);
        if (page.isEmpty()) requireUser(userId);
        return PageResponse.from(page.map(mapper::response));
    }

    @Transactional
    public NotificationResponse read(long id) {
        var n = notifications.findById(id).orElseThrow(() -> NotFoundException.of("Notification", id));
        n.markRead();
        return mapper.response(n);
    }

    @Transactional
    public ReadAllResponse readAll(long userId) {
        int updated = notifications.markAllRead(userId);
        if (updated == 0) requireUser(userId);
        return new ReadAllResponse(updated);
    }

    private void requireUser(long userId) {
        if (!users.existsById(userId)) throw NotFoundException.of("User", userId);
    }
}
