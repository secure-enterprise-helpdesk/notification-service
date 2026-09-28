package enterprise.notification_service.service;

import enterprise.notification_service.dto.TicketEvent;
import enterprise.notification_service.entity.Notification;
import enterprise.notification_service.repository.NotificationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final NotificationRepository notificationRepository;
    @Transactional
    public void createNotification(TicketEvent event) {
        switch (event.getEventType()) {
            case "TICKET_CREATED" -> create(
                    event,
                    event.getCreatedBy(),
                    "Ticket Created",
                    "Your ticket " + event.getTicketNumber() + " has been created."
            );
            case "TICKET_ASSIGNED" -> create(
                    event,
                    event.getAssignedTo(),
                    "Ticket Assigned",
                    "Ticket " + event.getTicketNumber() + " has been assigned to you."
            );
            case "TICKET_STATUS_CHANGED" -> create(
                    event,
                    event.getCreatedBy(),
                    "Ticket Status Updated",
                    "Ticket " + event.getTicketNumber()
                            + " status changed to " + event.getNewStatus() + "."
            );
            case "TICKET_COMMENT_ADDED" -> create(
                    event,
                    getCommentRecipient(event),
                    "New Ticket Comment",
                    "A new comment was added to ticket " + event.getTicketNumber() + "."
            );
            default -> log.warn(
                    "Unsupported ticket event: {}",
                    event.getEventType()
            );
        }
    }
    private void create(TicketEvent event, String recipientUserId, String title, String message) {
        if (recipientUserId == null || recipientUserId.isBlank()) return;
        if (notificationRepository.existsByEventIdAndRecipientUserId(event.getEventId(), recipientUserId)) return;
        Notification notification = Notification.builder()
                .notificationId(UUID.randomUUID())
                .recipientUserId(recipientUserId)
                .eventId(event.getEventId())
                .type(event.getEventType())
                .title(title)
                .message(message)
                .read(false)
                .build();
        notificationRepository.save(notification);
    }
    private String getCommentRecipient(TicketEvent event) {
        if (event.getCreatedBy() != null && !event.getCreatedBy().equals(event.getCommentedBy())) {
            return event.getCreatedBy();
        }
        return event.getAssignedTo();
    }
    public Page<Notification> getNotifications(String userId, Pageable pageable) {
        return notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId, pageable);
    }
    public Page<Notification> getUnreadNotifications(String userId, Pageable pageable) {
        return notificationRepository.findByRecipientUserIdAndReadFalseOrderByCreatedAtDesc(userId, pageable);
    }
    @Transactional
    public void markAsRead(UUID id, String userId) {
        Notification notification = notificationRepository.findByNotificationIdAndRecipientUserId(id, userId);
        if (notification == null) {
            return;
        }
        notification.setRead(true);
    }
}