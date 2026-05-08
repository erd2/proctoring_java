package com.aiu.proctoring.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Start session request (token-based for student).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StartSessionRequest {
    private String examToken;
}
