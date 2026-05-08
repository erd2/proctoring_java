package com.aiu.proctoring.domain.model;

import com.aiu.proctoring.domain.value.Email;
import com.aiu.proctoring.domain.value.UserId;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * User aggregate root representing system participants.
 * Roles: STUDENT, PROCTOR, ADMIN, SUPER_ADMIN
 */
@Entity
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"email"}),
    @UniqueConstraint(columnNames = {"username"})
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @EmbeddedId
    private UserId id;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 50, unique = true)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "google_id", length = 100, unique = true)
    private String googleId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_permissions", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "permission")
    @Builder.Default
    private Set<String> permissions = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public void updateLastLogin() {
        this.lastLogin = LocalDateTime.now();
    }

    public void addPermission(String permission) {
        this.permissions.add(permission);
    }

    public void removePermission(String permission) {
        this.permissions.remove(permission);
    }

    public void anonymize() {
        this.email = "anonymized_" + id.getValue() + "@deleted.com";
        this.username = "deleted_user_" + id.getValue();
        this.firstName = "Deleted";
        this.lastName = "User";
        this.phoneNumber = null;
        this.googleId = null;
        this.active = false;
    }

    public enum Role {
        STUDENT,
        PROCTOR,
        ADMIN,
        SUPER_ADMIN
    }
}
