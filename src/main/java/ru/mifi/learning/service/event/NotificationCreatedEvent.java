package ru.mifi.learning.service.event;

public record NotificationCreatedEvent(long notificationId, long recipientId) {}
