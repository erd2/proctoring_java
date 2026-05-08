package com.aiu.proctoring.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Violation type statistics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ViolationTypeStat {
    private String type;
    private Long count;
}
