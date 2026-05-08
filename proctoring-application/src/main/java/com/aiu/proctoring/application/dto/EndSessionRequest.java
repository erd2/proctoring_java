package com.aiu.proctoring.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * End session request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EndSessionRequest {
    private String sessionId;
    private String notes;
}
