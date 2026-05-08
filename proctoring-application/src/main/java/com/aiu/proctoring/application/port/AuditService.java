package com.aiu.proctoring.application.port;

import com.aiu.proctoring.application.dto.AuditLogDto;
import com.aiu.proctoring.application.dto.AuditSearchRequest;

import java.util.List;

/**
 * Port interface for audit logging operations.
 */
public interface AuditService {
    void log(AuditLogDto auditLogDto);
    List<AuditLogDto> search(AuditSearchRequest request);
    List<AuditLogDto> findByEntity(String entityType, String entityId);
    List<AuditLogDto> findByActor(String actorId, int limit);
}
