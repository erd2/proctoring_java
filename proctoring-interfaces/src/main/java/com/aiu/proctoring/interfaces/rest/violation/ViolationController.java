package com.aiu.proctoring.interfaces.rest.violation;

import com.aiu.proctoring.application.dto.*;
import com.aiu.proctoring.application.port.ViolationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Violation management endpoints.
 */
@RestController
@RequestMapping("/api/violations")
@RequiredArgsConstructor
public class ViolationController {

    private final ViolationService violationService;

    /**
     * Get all violations for a specific session.
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<ApiResponse<List<ViolationDto>>> getSessionViolations(
        @PathVariable String sessionId,
        @RequestAttribute("userId") String userId
    ) {
        try {
            List<ViolationDto> violations = violationService.getSessionViolations(sessionId);
            return ResponseEntity.ok(ApiResponse.success(null, violations));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to fetch violations: " + e.getMessage()));
        }
    }

    /**
     * Report a new violation manually (PROCTOR/ADMIN only).
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ViolationDto>> reportViolation(
        @RequestParam String sessionId,
        @RequestParam String studentId,
        @RequestParam String type,
        @RequestParam Double confidence,
        @RequestParam Long frameTimestamp,
        @RequestParam(required = false) String description,
        @RequestAttribute("userId") String reporterId
    ) {
        try {
            ViolationDto violation = violationService.reportViolation(
                sessionId, studentId, type, confidence, frameTimestamp, description);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Violation reported", violation));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to report violation: " + e.getMessage()));
        }
    }

    /**
     * Review a violation (mark as false positive or confirmed).
     */
    @PostMapping("/{violationId}/review")
    public ResponseEntity<ApiResponse<ViolationDto>> reviewViolation(
        @PathVariable String violationId,
        @RequestParam boolean isFalsePositive,
        @RequestParam(required = false) String notes,
        @RequestAttribute("userId") String reviewerId
    ) {
        try {
            ViolationDto violation = violationService.reviewViolation(
                violationId, reviewerId, isFalsePositive, notes);
            return ResponseEntity.ok(ApiResponse.success("Violation reviewed", violation));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to review violation: " + e.getMessage()));
        }
    }

    /**
     * Get violation statistics for a session.
     */
    @GetMapping("/stats/{sessionId}")
    public ResponseEntity<ApiResponse<ViolationStatisticsDto>> getStatistics(
        @PathVariable String sessionId,
        @RequestAttribute("userId") String userId
    ) {
        try {
            ViolationStatisticsDto stats = violationService.getViolationStatistics(sessionId);
            return ResponseEntity.ok(ApiResponse.success(null, stats));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to get statistics: " + e.getMessage()));
        }
    }
}
