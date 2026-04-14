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
     * Endpoint to trigger a notification based on a natural language string.
     * Example input: "send notification to 101 user for timeoff approval"
     */
    @PostMapping("/trigger")
    public ResponseEntity<String> triggerNotification(@RequestBody String rawMessage) {
        try {
            List<String> userIds = parseUserIds(rawMessage);
            String type = parseType(rawMessage);
            Integer companyId = com.w2w.api.config.TenantContext.getCurrentTenant();
            
            if (companyId == -1 || companyId == 0) {
                 // In some cases 0 might be system tenant, but for notifications we want a real company
                 log.warn("Trigger attempted without a valid tenant context.");
                 return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                         .body("Could not determine company context. Please ensure you are logged in.");
            }
            
            log.info("Parsed notification trigger: userIds={}, type='{}', companyId={}", userIds, type, companyId);
            
            NotificationRequest request = new NotificationRequest(userIds, type, rawMessage, companyId);
            notificationProducer.sendNotification(request);
            return ResponseEntity.ok("Notification triggered for users " + userIds + " of type " + type + " for company " + companyId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to parse message: " + e.getMessage());
        }
    }

    private List<String> parseUserIds(String message) {
        String lower = message.toLowerCase();
        
        // Scenario 2/3: Publish/Unpublish Schedule (All employees)
        if (lower.contains("published schedule") || lower.contains("unpublished schedule")) {
            return List.of("ALL_EMPLOYEES");
        }
        
        // Specific format: "for employee 101"
        if (lower.contains("for employee ")) {
            int start = lower.indexOf("for employee ") + "for employee ".length();
            int end = lower.indexOf(" ", start);
            if (end == -1) end = lower.length();
            return List.of(message.substring(start, end).trim());
        }

        // Standard format: "to 1, 2, 3 user"
        int toIndex = lower.indexOf("to ");
        int userIndex = lower.indexOf(" user");
        if (toIndex != -1 && userIndex != -1) {
            String idsPart = message.substring(toIndex + 3, userIndex).trim();
            return Arrays.stream(idsPart.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }

        throw new IllegalArgumentException("Target users not found in message. Please use 'to [ID] user' or 'for employee [ID]'");
    }

    private String parseType(String message) {
        String lower = message.toLowerCase();
        if (lower.contains("leave request") && lower.contains("approved")) {
            return "TIMEOFF_APPROVAL";
        }
        if (lower.contains("published schedule") && !lower.contains("un")) {
            return "SCHEDULE_PUBLISHED";
        }
        if (lower.contains("unpublished schedule") || (lower.contains("published") && lower.contains("un"))) {
            return "SCHEDULE_UNPUBLISHED";
        }
        return "GENERAL_NOTIFICATION";
    }
}
