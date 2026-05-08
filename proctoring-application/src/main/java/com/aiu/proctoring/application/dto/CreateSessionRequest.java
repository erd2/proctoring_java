package com.aiu.proctoring.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Exam session creation request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSessionRequest {
    private String studentId;
    private String disciplineCode;
    private String disciplineName;
    private LocalDateTime scheduledStart;
    private LocalDateTime scheduledEnd;
    private Integer maxViolations;
    private Double violationThreshold;
}
