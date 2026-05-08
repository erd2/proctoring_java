package com.aiu.proctoring.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Database health indicator.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatabaseHealthDto {
    private String status;
    private Long uptime;
}
