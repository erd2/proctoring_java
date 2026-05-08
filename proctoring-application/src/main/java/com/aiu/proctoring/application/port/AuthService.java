package com.aiu.proctoring.application.port;

import com.aiu.proctoring.application.dto.*;

import java.util.List;
import java.util.UUID;

/**
 * Port interface for authentication operations.
 */
public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    void logout(String token);
    AuthResponse refreshToken(String refreshToken);
    void validateToken(String token);
}
