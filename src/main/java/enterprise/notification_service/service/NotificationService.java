package enterprise.notification_service.service;

import enterprise.notification_service.dto.TicketEvent;
import enterprise.notification_service.entity.Notification;
import enterprise.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void createNotification(TicketEvent event) {
        switch (event.getEventType()) {
            case "TICKET_CREATED" -> create(
                    event,
                    event.getCreatedBy() != null ? event.getCreatedBy() : event.getUserId(),
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
            case "TICKET_COMMENT_ADDED" -> notifyRelevantParticipants(
                    event,
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
                .ticketNumber(event.getTicketNumber())
                .title(title)
                .message(message)
                .read(false)
                .build();

        notificationRepository.save(notification);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Notification delivery requires an active transaction");
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    messagingTemplate.convertAndSendToUser(
                            recipientUserId,
                            "/queue/notifications",
                            notification
                    );
                } catch (RuntimeException e) {
                    log.error("Failed to send notification {} to user {} over WebSocket",
                            notification.getNotificationId(), recipientUserId, e);
                }
            }
        });
    }

    private void notifyRelevantParticipants(TicketEvent event, String title, String message) {
        Set<String> recipients = new LinkedHashSet<>();

        if (event.getCreatedBy() != null && !event.getCreatedBy().isBlank()) {
            recipients.add(event.getCreatedBy());
        }
        if (event.getAssignedTo() != null && !event.getAssignedTo().isBlank()) {
            recipients.add(event.getAssignedTo());
        }
        if (event.getCommentedBy() != null && !event.getCommentedBy().isBlank()) {
            recipients.remove(event.getCommentedBy());
        }

        for (String recipientUserId : recipients) {
            create(event, recipientUserId, title, message);
        }
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