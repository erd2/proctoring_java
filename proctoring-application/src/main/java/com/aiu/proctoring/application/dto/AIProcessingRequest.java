package com.aiu.proctoring.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI processing request (async via Kafka).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIProcessingRequest {
    private String sessionId;
    private String studentId;
    private byte[] videoFrame;
    private String contentType;
    private Long frameTimestamp;
    private String aiModel;
}
