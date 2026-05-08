package com.aiu.proctoring.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * User DTO for responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private String id;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private String role;
    private boolean active;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
}
