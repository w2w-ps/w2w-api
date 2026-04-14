package com.w2w.api.notification;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private final NotificationService notificationService;

    public NotificationConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "${app.kafka.notification-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeNotification(NotificationRequest request) {
        System.out.println("Kafka Consumer received request: " + request);

        try {
            notificationService.processNotification(request);
        } catch (Exception e) {
            log.error("Error consuming notification: {}", e.getMessage());
        }
    }
}
