package com.aiu.proctoring.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka health indicator.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KafkaHealthDto {
    private String status;
    private String topic;
    private Integer partitions;
}
