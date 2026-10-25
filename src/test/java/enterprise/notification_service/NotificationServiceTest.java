package enterprise.notification_service;

import enterprise.notification_service.dto.TicketEvent;
import enterprise.notification_service.entity.Notification;
import enterprise.notification_service.repository.NotificationRepository;
import enterprise.notification_service.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationService notificationService;

    @BeforeEach
    void beginTransaction() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void clearTransaction() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void commentNotificationIsSavedAndPushedToOtherTicketParticipantOnly() {
        UUID eventId = UUID.randomUUID();
        TicketEvent event = new TicketEvent();
        event.setEventId(eventId);
        event.setEventType("TICKET_COMMENT_ADDED");
        event.setTicketNumber("TCK-TEST1234");
        event.setCreatedBy("USR-10001");
        event.setAssignedTo("USR-10003");
        event.setCommentedBy("USR-10003");
        when(notificationRepository.existsByEventIdAndRecipientUserId(eq(eventId), anyString())).thenReturn(false);

        notificationService.createNotification(event);

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(saved.capture());
        assertThat(saved.getValue().getRecipientUserId()).isEqualTo("USR-10001");
        assertThat(saved.getValue().getTicketNumber()).isEqualTo("TCK-TEST1234");
        verify(messagingTemplate, never()).convertAndSendToUser(
                anyString(),
                anyString(),
                any(Notification.class)
        );
        commitTransaction();
        verify(messagingTemplate).convertAndSendToUser(
                eq("USR-10001"),
                eq("/queue/notifications"),
                any(Notification.class)
        );
        verify(messagingTemplate, never()).convertAndSendToUser(
                eq("USR-10003"),
                anyString(),
                any(Notification.class)
        );
    }

    @Test
    void ticketCreatedUsesUserIdWhenCreatedByIsNotPresent() {
        TicketEvent event = new TicketEvent();
        event.setEventId(UUID.randomUUID());
        event.setEventType("TICKET_CREATED");
        event.setTicketNumber("TCK-TEST1234");
        event.setUserId("USR-10001");
        when(notificationRepository.existsByEventIdAndRecipientUserId(
                eq(event.getEventId()), eq("USR-10001"))).thenReturn(false);

        notificationService.createNotification(event);

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(saved.capture());
        assertThat(saved.getValue().getRecipientUserId()).isEqualTo("USR-10001");
        assertThat(saved.getValue().getTicketNumber()).isEqualTo("TCK-TEST1234");
        commitTransaction();
        verify(messagingTemplate).convertAndSendToUser(
                eq("USR-10001"),
                eq("/queue/notifications"),
                any(Notification.class)
        );
    }

    @Test
    void commentCreatesNotificationsForBothParticipantsOtherThanCommenter() {
        UUID eventId = UUID.randomUUID();
        TicketEvent event = new TicketEvent();
        event.setEventId(eventId);
        event.setEventType("TICKET_COMMENT_ADDED");
        event.setTicketNumber("TCK-TEST1234");
        event.setCreatedBy("USR-10001");
        event.setAssignedTo("USR-10003");
        event.setCommentedBy("USR-10002");
        when(notificationRepository.existsByEventIdAndRecipientUserId(eq(eventId), anyString()))
                .thenReturn(false);

        notificationService.createNotification(event);

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues())
                .extracting(Notification::getRecipientUserId)
                .containsExactly("USR-10001", "USR-10003");
        commitTransaction();
        verify(messagingTemplate, times(2)).convertAndSendToUser(
                anyString(),
                eq("/queue/notifications"),
                any(Notification.class)
        );
    }

    @Test
    void duplicateEventRecipientIsNotSavedOrSentAgain() {
        UUID eventId = UUID.randomUUID();
        TicketEvent event = new TicketEvent();
        event.setEventId(eventId);
        event.setEventType("TICKET_ASSIGNED");
        event.setTicketNumber("TCK-TEST1234");
        event.setAssignedTo("USR-10003");
        when(notificationRepository.existsByEventIdAndRecipientUserId(eventId, "USR-10003"))
                .thenReturn(true);

        notificationService.createNotification(event);

        verify(notificationRepository, never()).save(any(Notification.class));
        verify(messagingTemplate, never()).convertAndSendToUser(
                anyString(),
                anyString(),
                any(Notification.class)
        );
    }

    private void commitTransaction() {
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);
    }
}
