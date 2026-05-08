package com.aiu.proctoring.application.port;

import com.aiu.proctoring.application.dto.*;

import java.util.List;

/**
 * Port interface for violation management operations.
 */
public interface ViolationService {

    ViolationDto reportViolation(String sessionId, String studentId, String type, Double confidence,
                                  Long frameTimestamp, String description);

    List<ViolationDto> getSessionViolations(String sessionId);

    ViolationDto reviewViolation(String violationId, String reviewerId,
                                   boolean isFalsePositive, String notes);

    void processAIAnalysis(AIAnalysisResult analysisResult);

    ViolationStatisticsDto getViolationStatistics(String sessionId);
}
