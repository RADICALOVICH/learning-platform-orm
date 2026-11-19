package ru.mifi.learning.dto.notification;

import java.time.Instant;
import ru.mifi.learning.domain.enums.NotificationType;

public record NotificationResponse(
        Long id,
        Long recipientId,
        NotificationType type,
        String title,
        String message,
        boolean read,
        Instant createdAt) {}
