package com.w2w.api.notification;

import org.springframework.beans.factory.annotation.Autowired;
import com.w2w.api.config.TenantContext;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    @Autowired
    private NotificationService notificationService;

    @KafkaListener(topics = "${app.kafka.notification-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeNotification(NotificationRequest request) {
        log.info("Kafka Consumer received request: {}", request);

        try {
            // Transfer the tenant context from the message payload
            if (request.companyId() != null) {
                TenantContext.setCurrentTenant(request.companyId());
            }
            notificationService.processNotification(request);
        } catch (Exception e) {
            log.error("Error consuming notification: {}", e.getMessage());
        } finally {
            TenantContext.clear();
        }
    }
}
