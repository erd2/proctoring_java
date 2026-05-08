package com.aiu.proctoring.domain.model;

import com.aiu.proctoring.domain.value.UserId;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * AuditLog entity tracking all system actions for compliance and security.
 */
@Entity
@Table(name = "audit_logs")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", length = 100)
    private String entityId;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "actor_id", length = 100)
    private String actorId;

    @Column(name = "actor_type", length = 20)
    private String actorType;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "request_uri", length = 500)
    private String requestUri;

    @Column(name = "http_method", length = 10)
    private String httpMethod;

    @Column(name = "old_values", columnDefinition = "JSONB")
    private String oldValues;

    @Column(name = "new_values", columnDefinition = "JSONB")
    private String newValues;

    @Column(name = "metadata", columnDefinition = "JSONB")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public static AuditLog create(
        String entityType,
        String entityId,
        String action,
        String actorId,
        String actorType,
        String ipAddress,
        String userAgent,
        String requestUri,
        String httpMethod,
        String oldValues,
        String newValues,
        String metadata
    ) {
        return AuditLog.builder()
            .entityType(entityType)
            .entityId(entityId)
            .action(action)
            .actorId(actorId)
            .actorType(actorType)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .requestUri(requestUri)
            .httpMethod(httpMethod)
            .oldValues(oldValues)
            .newValues(newValues)
            .metadata(metadata)
            .build();
    }
}
