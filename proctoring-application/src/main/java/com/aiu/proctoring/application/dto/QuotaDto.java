package com.aiu.proctoring.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Quota DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuotaDto {
    private String userId;
    private Integer aiRequestLimit;
    private Integer aiRequestsUsed;
    private Integer aiRequestsRemaining;
    private LocalDateTime resetAt;
}
