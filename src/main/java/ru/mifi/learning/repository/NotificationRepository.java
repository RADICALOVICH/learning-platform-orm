package ru.mifi.learning.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import ru.mifi.learning.domain.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByRecipientId(Long recipientId, Pageable pageable);

    Page<Notification> findByRecipientIdAndReadFalse(Long recipientId, Pageable pageable);

    default Page<Notification> findByRecipientId(Long recipientId, boolean unreadOnly, Pageable pageable) {
        return unreadOnly
                ? findByRecipientIdAndReadFalse(recipientId, pageable)
                : findByRecipientId(recipientId, pageable);
    }

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            "update Notification n set n.read = true, n.updatedAt = instant, n.version = n.version + 1 where n.recipient.id = :recipientId and n.read = false")
    int markAllRead(Long recipientId);
}
