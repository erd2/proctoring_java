package com.aiu.proctoring.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Audit log search request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditSearchRequest {
    private String entityType;
    private String entityId;
    private String action;
    private String actorId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer limit;
    private Integer offset;
}
