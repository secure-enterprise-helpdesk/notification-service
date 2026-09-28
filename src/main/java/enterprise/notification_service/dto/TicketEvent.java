package enterprise.notification_service.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class TicketEvent {

    private UUID eventId;
    private String eventType;
    private Instant timestamp;
    private String ticketId;
    private String ticketNumber;
    private String createdBy;
    private String assignedTo;
    private String userId;
    private Integer departmentId;
    private String priority;
    private String oldStatus;
    private String newStatus;
    private String changedBy;
    private String commentedBy;
    private String comment;
}