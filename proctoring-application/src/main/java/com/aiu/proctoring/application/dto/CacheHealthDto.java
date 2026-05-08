package com.aiu.proctoring.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Redis cache health indicator.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CacheHealthDto {
    private String status;
    private Long connectedClients;
}
