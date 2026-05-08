package com.aiu.proctoring.application.service;

import com.aiu.proctoring.application.dto.AIAnalysisResult;
import com.aiu.proctoring.application.dto.AIProcessingRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI Analysis service using OpenAI Vision API.
 * Analyzes video frames for proctoring violations: multiple faces, phone usage, looking away, etc.
 */
@Service
@Slf4j
public class AIService {

    @Value("${ai.openai.api-key}")
    private String apiKey;

    @Value("${ai.openai.endpoint}")
    private String endpoint;

    @Value("${ai.openai.model}")
    private String model;

    private final RestTemplate restTemplate;

    public AIService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Analyze a video frame for proctoring violations.
     * @param request Contains frame data and metadata
     * @return AIAnalysisResult with detected violations
     */
    public AIAnalysisResult analyzeFrame(AIProcessingRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Encode image to base64
            String base64Image = Base64.getEncoder().encodeToString(request.getVideoFrame());

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);

            // System prompt for proctoring violation detection
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", """
                You are an AI proctoring system analyzing exam video frames.
                Detect the following violations:
                1. MULTIPLE_FACES - More than one person visible
                2. PHONE_USAGE - Student using phone or other electronic device
                3. LOOKING_AWAY - Student not looking at screen
                4. TALKING - Student talking to someone
                5. BOOK_REFERENCE - Student referring to books/notes
                6. HEADPHONES - Student wearing headphones
                7. UNAUTHORIZED_OBJECTS - Unauthorized objects detected
                """);

            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", List.of(
                Map.of("type", "text", "text", "Analyze this frame for proctoring violations."),
                Map.of(
                    "type", "image_url",
                    "image_url", Map.of("url", "data:image/jpeg;base64," + base64Image)
                )
            ));

            body.put("messages", List.of(systemMessage, userMessage));
            body.put("max_tokens", 300);
            body.put("temperature", 0.1); // Low temperature for consistency

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                endpoint,
                HttpMethod.POST,
                entity,
                Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("choices")) {
                @SuppressWarnings("unchecked")
                var choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (!choices.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    var message = (Map<String, String>) choices.get(0).get("message");
                    String content = message.get("content");
                    return parseAIResponse(content, request.getSessionId(), request.getFrameTimestamp());
                }
            }

            throw new RuntimeException("Invalid response from AI provider");

        } catch (HttpClientErrorException e) {
            log.error("OpenAI API error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("AI analysis failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("AI analysis exception", e);
            throw new RuntimeException("AI analysis failed", e);
        }
    }

    /**
     * Parse OpenAI response to extract violation data.
     */
    private AIAnalysisResult parseAIResponse(String content, String sessionId, Long frameTimestamp) {
        try {
            // Simple JSON extraction (in production use Jackson or Gson)
            content = content.replaceAll("\\n", " ").trim();
            String violationType = "NONE";
            double confidence = 0.0;
            String description = "No violation detected";

            if (content.contains("\"violation_type\"")) {
                int start = content.indexOf("\"violation_type\"") + 16;
                int end = content.indexOf(",", start);
                if (end > start) violationType = content.substring(start, end).replaceAll("\"", "").trim();
            }

            if (content.contains("\"confidence\"")) {
                int start = content.indexOf("\"confidence\"") + 12;
                int end = content.indexOf(",", start);
                if (end > start || content.indexOf("}", start) > start) {
                    end = end > start ? end : content.indexOf("}", start);
                    confidence = Double.parseDouble(content.substring(start, end).trim());
                }
            }

            if (content.contains("\"description\"")) {
                int start = content.indexOf("\"description\"") + 13;
                int end = content.lastIndexOf("\"");
                if (end > start) description = content.substring(start, end).replaceAll("\"", "").trim();
            }

            return AIAnalysisResult.builder()
                .sessionId(sessionId)
                .violationType(violationType)
                .confidence(confidence)
                .timestamp(frameTimestamp)
                .description(description)
                .rawResponse(content)
                .build();

        } catch (Exception e) {
            log.warn("Failed to parse AI response: {}", content, e);
            // Return no-violation as safe default
            return AIAnalysisResult.builder()
                .sessionId(sessionId)
                .violationType("NONE")
                .confidence(0.0)
                .timestamp(frameTimestamp)
                .description("Safe default - processing error")
                .rawResponse(content)
                .build();
        }
    }

    /**
     * Analyze base64-encoded image (alternative endpoint).
     */
    public AIAnalysisResult analyzeBase64Image(String base64Image, String sessionId, Long frameTimestamp) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);

            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", """
                You are an AI proctoring system. Detect academic integrity violations.
                Violation types: MULTIPLE_FACES, PHONE_USAGE, LOOKING_AWAY, TALKING, BOOK_REFERENCE, HEADPHONES, UNAUTHORIZED_OBJECTS.
                """);

            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", List.of(
                Map.of("type", "text", "text", "Check for proctoring violations."),
                Map.of("type", "image_url", "image_url", Map.of("url", "data:image/jpeg;base64," + base64Image))
            ));

            body.put("messages", List.of(systemMessage, userMessage));
            body.put("max_tokens", 200);
            body.put("temperature", 0.1);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                endpoint,
                HttpMethod.POST,
                entity,
                Map.class
            );

            var responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("choices")) {
                @SuppressWarnings("unchecked")
                var choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (!choices.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    var message = (Map<String, String>) choices.get(0).get("message");
                    return parseAIResponse(message.get("content"), sessionId, frameTimestamp);
                }
            }

            throw new RuntimeException("Invalid AI response");

        } catch (Exception e) {
            log.error("AI analysis failed for session {}: {}", sessionId, e.getMessage());
            throw e;
        }
    }
}



