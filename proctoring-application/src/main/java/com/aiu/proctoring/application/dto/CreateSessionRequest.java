package com.aiu.proctoring.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Exam session creation request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSessionRequest {
    private List<String> studentIds;
    private String groupName;
    private String disciplineCode;
    private String disciplineName;
    private LocalDateTime scheduledStart;
    private LocalDateTime scheduledEnd;
    private Integer maxViolations;
    private Double violationThreshold;
}
