package com.w2w.api.notification;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private final NotificationProducer notificationProducer;

    public NotificationController(NotificationProducer notificationProducer) {
        this.notificationProducer = notificationProducer;
    }

    /**
     * Endpoint to trigger a notification based on a structured request.
     */
    @PostMapping("/trigger")
    public ResponseEntity<String> triggerNotification(@RequestBody NotificationRequest request) {
        try {
            log.info("Processing notification trigger: task='{}'", request.task());
            notificationProducer.sendNotification(request);
            return ResponseEntity.ok("Notification triggered for task: " + request.task());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to process notification: " + e.getMessage());
        }
    }
}
