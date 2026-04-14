package com.w2w.api.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationProducer {

    private final KafkaTemplate<String, NotificationRequest> kafkaTemplate;

    @Value("${app.kafka.notification-topic}")
    private String topic;

    public NotificationProducer(KafkaTemplate<String, NotificationRequest> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendNotification(NotificationRequest request) {
        kafkaTemplate.send(topic, request);
        System.out.println("Notification sent to Kafka topic: " + topic + " for task: " + request.task());
    }
}
