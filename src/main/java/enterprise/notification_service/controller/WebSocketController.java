package enterprise.notification_service.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;

public class WebSocketController {
    @MessageMapping("/test")
    @SendTo("/topic/test")
    public String test(String message) {
        return message;
    }
}
