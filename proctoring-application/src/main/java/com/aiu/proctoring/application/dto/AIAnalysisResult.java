package com.aiu.proctoring.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI Analysis result.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIAnalysisResult {
    private String sessionId;
    private String studentId;
    private String violationType;
    private Double confidence;
    private Long timestamp;
    private String description;
    private String rawResponse;
}
