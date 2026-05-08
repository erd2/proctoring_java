package com.aiu.proctoring.infrastructure.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Kafka message producer for AI processing requests.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String AI_REQUEST_TOPIC = "ai-processing-requests";

    /**
     * Send AI processing request to Kafka.
     */
    public void sendMessage(String topic, String key, Object message) {
        try {
            kafkaTemplate.send(topic, key, message);
            log.debug("Sent message to topic {} with key {}", topic, key);
        } catch (Exception e) {
            log.error("Failed to send Kafka message to topic {}: {}", topic, e.getMessage(), e);
            throw new RuntimeException("Kafka send failed", e);
        }
    }

    public void sendAIRequest(String sessionId, Object request) {
        sendMessage(AI_REQUEST_TOPIC, sessionId, request);
    }
}
