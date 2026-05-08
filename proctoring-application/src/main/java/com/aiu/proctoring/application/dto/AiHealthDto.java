package com.aiu.proctoring.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI service health indicator.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiHealthDto {
    private String status;
    private Double avgResponseTime;
    private Long totalRequests;
}
