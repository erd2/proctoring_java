package com.aiu.proctoring.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Violation DTO for response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ViolationDto {
    private String id;
    private String sessionId;
    private String studentId;
    private String type;
    private Double confidence;
    private Long frameTimestamp;
    private String description;
    private LocalDateTime detectedAt;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private Boolean isFalsePositive;
    private String notes;
}
