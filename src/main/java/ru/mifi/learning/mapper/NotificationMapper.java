package ru.mifi.learning.mapper;

import org.springframework.stereotype.Component;
import ru.mifi.learning.domain.entity.Notification;
import ru.mifi.learning.dto.notification.NotificationResponse;

@Component
public class NotificationMapper {
    public NotificationResponse response(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getRecipient().getId(),
                n.getType(),
                n.getTitle(),
                n.getMessage(),
                n.isRead(),
                n.getCreatedAt());
    }
}
