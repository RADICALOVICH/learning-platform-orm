package ru.mifi.learning.service.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class EmailStubListener {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotification(NotificationCreatedEvent event) {
        log.info("Демо-email: notificationId={}, recipientId={}", event.notificationId(), event.recipientId());
    }
}
