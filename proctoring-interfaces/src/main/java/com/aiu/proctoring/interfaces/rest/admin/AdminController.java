package com.aiu.proctoring.interfaces.rest.admin;

import com.aiu.proctoring.application.dto.*;
import com.aiu.proctoring.application.port.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Administrative management endpoints (ADMIN role only).
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * List all users (filterable by role and active status).
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserDto>>> listUsers(
        @RequestParam(required = false) String role,
        @RequestParam(required = false) Boolean active,
        @RequestAttribute("userId") String adminId
    ) {
        try {
            List<UserDto> users = adminService.listUsers(role, active);
            return ResponseEntity.ok(ApiResponse.success(null, users));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to fetch users: " + e.getMessage()));
        }
    }

    /**
     * Update user active status.
     */
    @PutMapping("/users/{userId}/status")
    public ResponseEntity<ApiResponse<Void>> updateUserStatus(
        @PathVariable String userId,
        @RequestParam boolean active,
        @RequestAttribute("userId") String adminId
    ) {
        try {
            adminService.updateUserStatus(userId, active, adminId);
            return ResponseEntity.ok(ApiResponse.success("User status updated", null));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to update user: " + e.getMessage()));
        }
    }

    /**
     * Anonymize/delete user data (GDPR compliance).
     */
    @DeleteMapping("/users/{userId}/data")
    public ResponseEntity<ApiResponse<Void>> deleteUserData(
        @PathVariable String userId,
        @RequestAttribute("userId") String adminId
    ) {
        try {
            adminService.deleteUserData(userId, adminId);
            return ResponseEntity.ok(ApiResponse.success("User data deleted", null));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to delete user data: " + e.getMessage()));
        }
    }

    /**
     * Search audit logs with filters.
     */
    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<List<AuditLogDto>>> auditLogs(
        @RequestParam(required = false) String entityType,
        @RequestParam(required = false) String entityId,
        @RequestParam(required = false) String action,
        @RequestParam(required = false) String actorId,
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate,
        @RequestParam(required = false, defaultValue = "100") Integer limit,
        @RequestAttribute("userId") String adminId
    ) {
        try {
            AuditSearchRequest request = AuditSearchRequest.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .actorId(actorId)
                .startDate(startDate != null ? LocalDateTime.parse(startDate) : null)
                .endDate(endDate != null ? LocalDateTime.parse(endDate) : null)
                .limit(limit)
                .offset(0)
                .build();

            List<AuditLogDto> logs = adminService.searchAuditLogs(request);
            return ResponseEntity.ok(ApiResponse.success(null, logs));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to fetch audit logs: " + e.getMessage()));
        }
    }

    /**
     * Get system health status.
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<SystemHealthDto>> systemHealth(
        @RequestAttribute("userId") String adminId
    ) {
        try {
            SystemHealthDto health = adminService.getSystemHealth();
            return ResponseEntity.ok(ApiResponse.success(null, health));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error("Failed to get system health: " + e.getMessage()));
        }
    }

    /**
     * Get user quota (AI request limits).
     */
    @GetMapping("/users/{userId}/quota")
    public ResponseEntity<ApiResponse<QuotaDto>> getUserQuota(
        @PathVariable String userId,
        @RequestAttribute("userId") String adminId
    ) {
        try {
            QuotaDto quota = adminService.getUserQuota(userId);
            return ResponseEntity.ok(ApiResponse.success(null, quota));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to get quota: " + e.getMessage()));
        }
    }

    /**
     * Update user quota.
     */
    @PutMapping("/users/{userId}/quota")
    public ResponseEntity<ApiResponse<QuotaDto>> updateUserQuota(
        @PathVariable String userId,
        @RequestParam Integer aiRequestLimit,
        @RequestAttribute("userId") String adminId
    ) {
        try {
            QuotaDto quota = adminService.updateUserQuota(userId, aiRequestLimit, adminId);
            return ResponseEntity.ok(ApiResponse.success("Quota updated", quota));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to update quota: " + e.getMessage()));
        }
    }

    /**
     * Generate system report.
     */
    @PostMapping("/reports")
    public ResponseEntity<ApiResponse<String>> generateReport(
        @Valid @RequestBody ReportRequest request,
        @RequestAttribute("userId") String adminId
    ) {
        try {
            String reportId = adminService.generateReport(request);
            return ResponseEntity.accepted()
                .body(ApiResponse.success("Report generation started", reportId));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to generate report: " + e.getMessage()));
        }
    }
}
