package com.aiu.proctoring.application.service;

import com.aiu.proctoring.application.dto.AuthResponse;
import com.aiu.proctoring.application.dto.LoginRequest;
import com.aiu.proctoring.application.dto.RegisterRequest;
import com.aiu.proctoring.application.dto.UserDto;
import com.aiu.proctoring.application.port.AuthService;
import com.aiu.proctoring.domain.exception.DomainException;
import com.aiu.proctoring.domain.model.User;
import com.aiu.proctoring.domain.value.UserId;
import com.aiu.proctoring.infrastructure.repository.UserRepository;
import com.aiu.proctoring.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DomainException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DomainException("Email already registered");
        }

        User.Role role = parseRole(request.getRole());
        if (!isValidStudentOrProctorRole(role)) {
            throw new DomainException("Self-registration only allowed for STUDENT or PROCTOR roles");
        }

        User user = User.builder()
            .id(UserId.generate())
            .email(request.getEmail())
            .username(request.getUsername())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .phoneNumber(request.getPhoneNumber())
            .role(role)
            .build();

        userRepository.save(user);

        // Generate JWT tokens
        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        String accessToken = jwtService.generateToken(auth);
        String refreshToken = jwtService.generateRefreshToken(auth);

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .expiresIn(Long.parseLong(jwtService.getExpiration()))
            .tokenType("Bearer")
            .user(mapToDto(user))
            .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new DomainException("User not found"));

        user.updateLastLogin();
        userRepository.save(user);

        String accessToken = jwtService.generateToken(auth);
        String refreshToken = jwtService.generateRefreshToken(auth);

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .expiresIn(Long.parseLong(jwtService.getExpiration()))
            .tokenType("Bearer")
            .user(mapToDto(user))
            .build();
    }

    @Override
    public void logout(String token) {
        // Invalidate token on blacklist (implement if needed)
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        String username = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new DomainException("User not found"));

        // Build authorities from role and permissions
        List<GrantedAuthority> authorities = new java.util.ArrayList<>();
        authorities.add(() -> "ROLE_" + user.getRole().name());
        user.getPermissions().forEach(perm -> authorities.add(() -> perm));

        Authentication auth = new UsernamePasswordAuthenticationToken(
            username,
            null,
            authorities
        );

        String newAccessToken = jwtService.generateToken(auth);
        String newRefreshToken = jwtService.generateRefreshToken(auth);

        return AuthResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .expiresIn(Long.parseLong(jwtService.getExpiration()))
            .tokenType("Bearer")
            .user(mapToDto(user))
            .build();
    }

    @Override
    public void validateToken(String token) {
        jwtService.validateToken(token);
    }

    private User.Role parseRole(String roleStr) {
        try {
            return User.Role.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DomainException("Invalid role: " + roleStr);
        }
    }

    private boolean isValidStudentOrProctorRole(User.Role role) {
        return role == User.Role.STUDENT || role == User.Role.PROCTOR;
    }

    private UserDto mapToDto(User user) {
        return UserDto.builder()
            .id(user.getId().getValue())
            .email(user.getEmail())
            .username(user.getUsername())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .role(user.getRole().name())
            .active(user.isActive())
            .lastLogin(user.getLastLogin())
            .createdAt(user.getCreatedAt())
            .build();
    }
}
