package com.aiu.proctoring.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Report generation request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportRequest {
    private String proctorId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String format; // JSON, PDF
    private Boolean includeFalsePositives;
}
