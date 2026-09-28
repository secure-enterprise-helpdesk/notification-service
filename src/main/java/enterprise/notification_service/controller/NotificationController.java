package enterprise.notification_service.controller;

import enterprise.notification_service.entity.Notification;
import enterprise.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<Page<Notification>> getNotifications(Authentication authentication, Pageable pageable) {
        String userId = authentication.getName();
        return ResponseEntity.ok(notificationService.getNotifications(userId, pageable));
    }

    @GetMapping("/unread")
    public ResponseEntity<Page<Notification>> getUnreadNotifications(Authentication authentication, Pageable pageable) {
         String userId = authentication.getName();
        return ResponseEntity.ok(notificationService.getUnreadNotifications(userId, pageable));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID id, Authentication authentication) {
        String userId = authentication.getName();
        notificationService.markAsRead(id, userId);
        return ResponseEntity.noContent().build();
    }
}