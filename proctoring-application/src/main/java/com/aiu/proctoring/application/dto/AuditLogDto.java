package com.aiu.proctoring.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Audit log DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogDto {
    private Long id;
    private String entityType;
    private String entityId;
    private String action;
    private String actorId;
    private String actorType;
    private String ipAddress;
    private String userAgent;
    private String requestUri;
    private String httpMethod;
    private String oldValues;
    private String newValues;
    private String metadata;
    private LocalDateTime createdAt;
}
