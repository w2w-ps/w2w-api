package com.w2w.api.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class NotificationProducer {

    private static final Logger log = LoggerFactory.getLogger(NotificationProducer.class);

    private final KafkaTemplate<String, NotificationRequest> kafkaTemplate;

    @Value("${app.kafka.notification-topic}")
    private String topic;

    public NotificationProducer(KafkaTemplate<String, NotificationRequest> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendNotification(NotificationRequest request) {
        kafkaTemplate.send(topic, request);
        log.info("Notification sent to Kafka topic: {} for task: {}", topic, request.task());
    }
}
