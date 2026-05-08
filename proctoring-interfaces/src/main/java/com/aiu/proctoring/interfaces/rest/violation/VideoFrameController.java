package com.aiu.proctoring.interfaces.rest.violation;

import com.aiu.proctoring.application.dto.AIProcessingRequest;
import com.aiu.proctoring.application.dto.ApiResponse;
import com.aiu.proctoring.domain.exception.DomainException;
import com.aiu.proctoring.infrastructure.cache.RateLimiterService;
import com.aiu.proctoring.infrastructure.kafka.KafkaProducerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * Video frame upload endpoint for AI analysis.
 * Students/proctors upload video frames; processed asynchronously via Kafka.
 */
@RestController
@RequestMapping("/api/frames")
@RequiredArgsConstructor
@Slf4j
public class VideoFrameController {

    private final KafkaProducerService kafkaProducerService;
    private final RateLimiterService rateLimiterService;

    private static final String AI_REQUEST_TOPIC = "ai-processing-requests";

    /**
     * Upload a video frame for AI analysis.
     * Requires authentication as STUDENT and ownership of session.
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<String>> uploadFrame(
        @RequestParam("sessionId") String sessionId,
        @RequestParam("frameTimestamp") Long frameTimestamp,
        @RequestParam("studentId") String studentId,
        @RequestParam("file") MultipartFile file,
        HttpServletRequest request
    ) {
        try {
            // Rate limiting check
            if (!rateLimiterService.isAllowed(studentId, 60, 60)) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.error("Rate limit exceeded. Max 60 frames per minute."));
            }

            // Validate file
            validateFrame(file);

            // Authorization: userId from JWT filter must match studentId in form
            String userId = (String) request.getAttribute("userId");
            if (userId == null || !userId.equals(studentId)) {
                throw new DomainException("Unauthorized: student ID mismatch");
            }

            AIProcessingRequest aiRequest = AIProcessingRequest.builder()
                .sessionId(sessionId)
                .studentId(studentId)
                .videoFrame(file.getBytes())
                .contentType(file.getContentType())
                .frameTimestamp(frameTimestamp)
                .aiModel("gpt-4o-mini")
                .build();

            // Send to Kafka for async processing
            kafkaProducerService.sendAIRequest(sessionId, aiRequest);

            return ResponseEntity.accepted()
                .body(ApiResponse.success("Frame queued for AI analysis", null));
        } catch (DomainException e) {
            log.warn("Authorization error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(e.getMessage()));
        } catch (IOException e) {
            log.error("Failed to read frame file", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to process frame"));
        }
    }

    /**
     * Upload base64-encoded frame (alternative for frontend).
     */
    @PostMapping("/base64")
    public ResponseEntity<ApiResponse<String>> uploadBase64(
        @Valid @RequestBody FrameUploadRequest request,
        HttpServletRequest servletRequest
    ) {
        try {
            // Rate limiting check
            if (!rateLimiterService.isAllowed(request.getStudentId(), 60, 60)) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.error("Rate limit exceeded. Max 60 frames per minute."));
            }

            // Authorization check
            String userId = (String) servletRequest.getAttribute("userId");
            if (userId == null || !userId.equals(request.getStudentId())) {
                throw new DomainException("Unauthorized: student ID mismatch");
            }

            byte[] imageBytes = java.util.Base64.getDecoder().decode(request.getImageBase64());

            AIProcessingRequest aiRequest = AIProcessingRequest.builder()
                .sessionId(request.getSessionId())
                .studentId(request.getStudentId())
                .videoFrame(imageBytes)
                .contentType("image/jpeg")
                .frameTimestamp(request.getFrameTimestamp())
                .aiModel("gpt-4o-mini")
                .build();

            kafkaProducerService.sendAIRequest(request.getSessionId(), aiRequest);

            return ResponseEntity.accepted()
                .body(ApiResponse.success("Frame queued for AI analysis", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Invalid request: " + e.getMessage()));
        } catch (DomainException e) {
            log.warn("Authorization error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    private void validateFrame(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (file.getSize() > 5 * 1024 * 1024) { // 5 MB limit
            throw new IllegalArgumentException("File too large (>5MB)");
        }
        String contentType = file.getContentType();
        if (contentType == null ||
            !(contentType.startsWith("image/") || contentType.startsWith("video/"))) {
            throw new IllegalArgumentException("Unsupported media type");
        }
    }

    /**
     * Request DTO for base64 upload.
     */
    @lombok.Data
    static class FrameUploadRequest {
        private String sessionId;
        private String studentId;
        private String imageBase64;
        private Long frameTimestamp;
    }
}
