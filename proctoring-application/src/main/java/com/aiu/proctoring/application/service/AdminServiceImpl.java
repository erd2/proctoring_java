package com.aiu.proctoring.application.service;

import com.aiu.proctoring.application.dto.*;
import com.aiu.proctoring.application.port.AdminService;
import com.aiu.proctoring.application.port.AuditService;
import com.aiu.proctoring.domain.exception.DomainException;
import com.aiu.proctoring.domain.model.User;
import com.aiu.proctoring.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> listUsers(String role, Boolean active) {
        List<User> users;
        if (role != null && active != null) {
            users = userRepository.findAll().stream()
                .filter(u -> u.getRole().name().equalsIgnoreCase(role))
                .filter(u -> u.isActive() == active)
                .collect(Collectors.toList());
        } else if (role != null) {
            users = userRepository.findAll().stream()
                .filter(u -> u.getRole().name().equalsIgnoreCase(role))
                .collect(Collectors.toList());
        } else if (active != null) {
            users = userRepository.findAll().stream()
                .filter(u -> u.isActive() == active)
                .collect(Collectors.toList());
        } else {
            users = userRepository.findAll();
        }

        return users.stream()
            .map(this::mapToUserDto)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateUserStatus(String userId, boolean active, String adminId) {
        User user = userRepository.findById(com.aiu.proctoring.domain.value.UserId.from(userId))
            .orElseThrow(() -> new DomainException("User not found"));

        User.Role role = user.getRole();
        if (role == User.Role.SUPER_ADMIN && !adminId.equals(userId)) {
            throw new DomainException("Cannot modify another SUPER_ADMIN");
        }

        if (active) {
            user.activate();
        } else {
            user.deactivate();
        }

        userRepository.save(user);

        auditService.log(AuditLogDto.builder()
            .entityType("USER")
            .entityId(userId)
            .action("UPDATE_STATUS")
            .actorId(adminId)
            .actorType("ADMIN")
            .build());
    }

    @Override
    @Transactional
    public void deleteUserData(String userId, String adminId) {
        User user = userRepository.findById(com.aiu.proctoring.domain.value.UserId.from(userId))
            .orElseThrow(() -> new DomainException("User not found"));

        // Check 30-day retention policy
        if (user.getCreatedAt().isAfter(LocalDateTime.now().minusDays(30))) {
            throw new DomainException("Cannot delete user data within 30 days of creation");
        }

        // Anonymize user
        user.anonymize();
        userRepository.save(user);

        auditService.log(AuditLogDto.builder()
            .entityType("USER")
            .entityId(userId)
            .action("ANONYMIZE")
            .actorId(adminId)
            .actorType("ADMIN")
            .build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogDto> searchAuditLogs(AuditSearchRequest request) {
        // Implementation with pagination and filters
        return auditService.search(request);
    }

    @Override
    @Transactional(readOnly = true)
    public SystemHealthDto getSystemHealth() {
        return SystemHealthDto.builder()
            .status("UP")
            .database(new DatabaseHealthDto("UP", System.currentTimeMillis() / 1000))
            .cache(new CacheHealthDto("UP", 0L))
            .kafka(new KafkaHealthDto("UP", "", 1))
            .ai(new AiHealthDto("UP", 0.5, 0L))
            .timestamp(LocalDateTime.now().toString())
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public QuotaDto getUserQuota(String userId) {
        User user = userRepository.findById(com.aiu.proctoring.domain.value.UserId.from(userId))
            .orElseThrow(() -> new DomainException("User not found"));

        return QuotaDto.builder()
            .userId(userId)
            .aiRequestLimit(60)
            .aiRequestsUsed(0)
            .aiRequestsRemaining(60)
            .resetAt(LocalDateTime.now().plusMinutes(1))
            .build();
    }

    @Override
    @Transactional
    public QuotaDto updateUserQuota(String userId, Integer aiRequestLimit, String adminId) {
        User user = userRepository.findById(com.aiu.proctoring.domain.value.UserId.from(userId))
            .orElseThrow(() -> new DomainException("User not found"));

        if (aiRequestLimit < 1 || aiRequestLimit > 1000) {
            throw new DomainException("Invalid AI request limit (1-1000)");
        }

        auditService.log(AuditLogDto.builder()
            .entityType("USER_QUOTA")
            .entityId(userId)
            .action("UPDATE_QUOTA")
            .actorId(adminId)
            .actorType("ADMIN")
            .newValues("{\"limit\": " + aiRequestLimit + "}")
            .build());

        return QuotaDto.builder()
            .userId(userId)
            .aiRequestLimit(aiRequestLimit)
            .aiRequestsUsed(0)
            .aiRequestsRemaining(aiRequestLimit)
            .resetAt(LocalDateTime.now().plusMinutes(1))
            .build();
    }

    @Override
    @Transactional
    public String generateReport(ReportRequest request) {
        String reportId = java.util.UUID.randomUUID().toString();
        auditService.log(AuditLogDto.builder()
            .entityType("REPORT")
            .entityId(reportId)
            .action("GENERATE")
            .actorId(request.getProctorId())
            .actorType(request.getFormat())
            .build());
        return reportId;
    }

    private UserDto mapToUserDto(User user) {
        return UserDto.builder()
            .id(user.getId().getValue())
            .email(user.getEmail())
            .username(user.getUsername())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .role(user.getRole().name())
            .active(user.isActive())
            .createdAt(user.getCreatedAt())
            .build();
    }
}
