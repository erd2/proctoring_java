package com.aiu.proctoring.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Violation statistics DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ViolationStatisticsDto {
    private String sessionId;
    private Long totalViolations;
    private Long reviewedViolations;
    private Long falsePositives;
    private Double averageConfidence;
    private List<ViolationTypeStat> byType;
    private List<TimeSeriesStat> timeSeries;
}
