package enterprise.notification_service.consumer;

import enterprise.notification_service.service.NotificationService;
import tools.jackson.databind.ObjectMapper;
import enterprise.notification_service.dto.TicketEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketEventConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    @KafkaListener(
            topics = "ticket-events",
            groupId = "notification-service"
    )
    public void consume(String message) {
        try {
            TicketEvent event = objectMapper.readValue(message, TicketEvent.class);
            notificationService.createNotification(event);
        } catch (Exception e) {
            log.error("Failed to process Kafka event: {}", message, e);
        }
    }
}