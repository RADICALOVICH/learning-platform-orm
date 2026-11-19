package ru.mifi.learning.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import ru.mifi.learning.domain.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    @Query("""
            select n from Notification n where n.recipient.id = :recipientId and (:unreadOnly = false or n.read = false)
            """)
    Page<Notification> findByRecipientId(Long recipientId, boolean unreadOnly, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            "update Notification n set n.read = true, n.version = n.version + 1 where n.recipient.id = :recipientId and n.read = false")
    int markAllRead(Long recipientId);
}
