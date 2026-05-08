package com.aiu.proctoring.application.kafka;

import com.aiu.proctoring.application.dto.AIAnalysisResult;
import com.aiu.proctoring.application.dto.AIProcessingRequest;
import com.aiu.proctoring.application.service.AIService;
import com.aiu.proctoring.application.port.ViolationService;
import com.aiu.proctoring.domain.model.Violation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Kafka message processor for AI analysis tasks.
 * Consumes video frames from queue, processes via AI, and produces results.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AIKafkaProcessor {

    private final AIService aiService;
    private final ViolationService violationService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String AI_REQUEST_TOPIC = "ai-processing-requests";
    private static final String AI_RESULT_TOPIC = "ai-analysis-results";
    private static final String VIOLATION_TOPIC = "violations-detected";

    /**
     * Listen for AI processing requests.
     */
    @KafkaListener(topics = AI_REQUEST_TOPIC, groupId = "ai-processor-group")
    public void processAIRequest(AIProcessingRequest request) {
        log.info("Processing AI request for session: {}", request.getSessionId());
        try {
            // Call AI service
            AIAnalysisResult result = aiService.analyzeFrame(request);

            // Schedule message for further processing
            kafkaTemplate.send(AI_RESULT_TOPIC, result);

            // If violation detected, also notify violation handler
            if (!"NONE".equals(result.getViolationType()) &&
                result.getConfidence() > 0.7) { // Threshold

                Violation violation = Violation.builder()
                    .id(com.aiu.proctoring.domain.value.ViolationId.generate())
                    .type(result.getViolationType())
                    .confidence(result.getConfidence())
                    .frameTimestamp(result.getTimestamp())
                    .description(result.getDescription())
                    .session(null) // Will be set by handler
                    .detectedAt(java.time.LocalDateTime.now())
                    .build();

                kafkaTemplate.send(VIOLATION_TOPIC, violation);
            }

        } catch (Exception e) {
            log.error("Failed to process AI request for session {}: {}",
                request.getSessionId(), e.getMessage(), e);
            // Send to DLQ or retry queue
        }
    }

    /**
     * Handle AI analysis results and persist violations.
     */
    @KafkaListener(topics = AI_RESULT_TOPIC, groupId = "violation-processor-group")
    public void handleAIResult(AIAnalysisResult result) {
        log.debug("AI result for session {}: {} confidence={}",
            result.getSessionId(), result.getViolationType(), result.getConfidence());

        try {
            violationService.processAIAnalysis(result);
        } catch (Exception e) {
            log.error("Failed to persist violation for session {}: {}",
                result.getSessionId(), e.getMessage(), e);
        }
    }

    /**
     * Process detected violations and send notifications.
     */
    @KafkaListener(topics = VIOLATION_TOPIC, groupId = "violation-notifier-group")
    public void handleViolation(Violation violation) {
        log.warn("Violation detected: type={}, confidence={}, session={}",
            violation.getType(), violation.getConfidence(), violation.getSession());

        // Send notification to proctor, update session status, etc.
        // This could be delegated to a NotificationService
    }
}
