package com.aiu.proctoring.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * System health DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemHealthDto {
    private String status;
    private DatabaseHealthDto database;
    private CacheHealthDto cache;
    private KafkaHealthDto kafka;
    private AiHealthDto ai;
    private String timestamp;
}
