package com.aiu.proctoring.application.port;

import com.aiu.proctoring.application.dto.*;

import java.util.List;

/**
 * Port interface for administrative operations.
 */
public interface AdminService {

    List<UserDto> listUsers(String role, Boolean active);

    void updateUserStatus(String userId, boolean active, String adminId);

    void deleteUserData(String userId, String adminId);

    List<AuditLogDto> searchAuditLogs(AuditSearchRequest request);

    SystemHealthDto getSystemHealth();

    QuotaDto getUserQuota(String userId);

    QuotaDto updateUserQuota(String userId, Integer aiRequestLimit, String adminId);

    String generateReport(ReportRequest request);
}
