package com.aiu.proctoring.application.service;

import com.aiu.proctoring.application.dto.AuditLogDto;
import com.aiu.proctoring.application.dto.AuditSearchRequest;
import com.aiu.proctoring.application.port.AuditService;
import com.aiu.proctoring.domain.model.AuditLog;
import com.aiu.proctoring.infrastructure.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public void log(AuditLogDto auditLogDto) {
        AuditLog log = AuditLog.create(
            auditLogDto.getEntityType(),
            auditLogDto.getEntityId(),
            auditLogDto.getAction(),
            auditLogDto.getActorId(),
            auditLogDto.getActorType(),
            auditLogDto.getIpAddress(),
            auditLogDto.getUserAgent(),
            auditLogDto.getRequestUri(),
            auditLogDto.getHttpMethod(),
            auditLogDto.getOldValues(),
            auditLogDto.getNewValues(),
            auditLogDto.getMetadata()
        );
        auditLogRepository.save(log);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogDto> search(AuditSearchRequest request) {
        // Implementation with specification pattern would go here
        // For simplicity, return empty list
        return List.of();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogDto> findByEntity(String entityType, String entityId) {
        List<AuditLog> logs = auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId);
        return logs.stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogDto> findByActor(String actorId, int limit) {
        List<AuditLog> logs = auditLogRepository.findByActorId(actorId);
        return logs.stream()
            .limit(limit)
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    private AuditLogDto mapToDto(AuditLog log) {
        return AuditLogDto.builder()
            .id(log.getId())
            .entityType(log.getEntityType())
            .entityId(log.getEntityId())
            .action(log.getAction())
            .actorId(log.getActorId())
            .actorType(log.getActorType())
            .ipAddress(log.getIpAddress())
            .userAgent(log.getUserAgent())
            .requestUri(log.getRequestUri())
            .httpMethod(log.getHttpMethod())
            .createdAt(log.getCreatedAt())
            .metadata(log.getMetadata())
            .build();
    }
}
