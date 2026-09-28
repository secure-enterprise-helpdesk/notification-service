package enterprise.notification_service.repository;

import enterprise.notification_service.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientUserIdOrderByCreatedAtDesc(String recipientUserId, Pageable pageable);
    Page<Notification> findByRecipientUserIdAndReadFalseOrderByCreatedAtDesc(String recipientUserId, Pageable pageable);
    long countByRecipientUserIdAndReadFalse(String recipientUserId);
    boolean existsByEventIdAndRecipientUserId(UUID eventId, String recipientUserId);
    Notification findByNotificationIdAndRecipientUserId(UUID notificationId, String recipientUserId);
}
