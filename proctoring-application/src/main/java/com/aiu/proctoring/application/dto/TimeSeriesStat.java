package com.aiu.proctoring.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Time-series statistics entry.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeSeriesStat {
    private String hour;
    private Long count;
}
