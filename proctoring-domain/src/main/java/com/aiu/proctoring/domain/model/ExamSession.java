package com.aiu.proctoring.domain.model;

import com.aiu.proctoring.domain.value.ExamSessionId;
import com.aiu.proctoring.domain.value.UserId;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ExamSession aggregate root representing a proctoring session.
 * Manages lifecycle from creation to completion with violation tracking.
 */
@Entity
@Table(name = "exam_sessions")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamSession {

    @EmbeddedId
    private ExamSessionId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proctor_id", nullable = false)
    private User proctor;

    @Column(name = "group_name", length = 200)
    private String groupName;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "exam_session_participants",
        joinColumns = @JoinColumn(name = "session_id"),
        inverseJoinColumns = @JoinColumn(name = "student_id")
    )
    @Builder.Default
    private List<User> participants = new ArrayList<>();

    @Column(name = "discipline_code", nullable = false, length = 100)
    private String disciplineCode;

    @Column(name = "discipline_name", nullable = false, length = 200)
    private String disciplineName;

    @Column(name = "exam_token", nullable = false, length = 64, unique = true)
    private String examToken;

    @Column(name = "scheduled_start", nullable = false)
    private LocalDateTime scheduledStart;

    @Column(name = "scheduled_end", nullable = false)
    private LocalDateTime scheduledEnd;

    @Column(name = "actual_start")
    private LocalDateTime actualStart;

    @Column(name = "actual_end")
    private LocalDateTime actualEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "max_violations")
    private Integer maxViolations;

    @Column(name = "violation_threshold")
    private Double violationThreshold;

    @Column(name = "ai_model_used", length = 50)
    private String aiModelUsed;

    @OneToMany(mappedBy = "session", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Violation> violations = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = Status.CREATED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void start() {
        if (status == Status.ACTIVE) {
            return;
        }
        if (status != Status.CREATED) {
            throw new IllegalStateException("Cannot start session in " + status + " state");
        }
        this.actualStart = LocalDateTime.now();
        this.status = Status.ACTIVE;
    }

    public void end() {
        if (status == Status.COMPLETED) {
            return;
        }
        if (status != Status.ACTIVE) {
            throw new IllegalStateException("Cannot end session in " + status + " state");
        }
        this.actualEnd = LocalDateTime.now();
        this.status = Status.COMPLETED;
    }

    public void markViolated(Violation violation) {
        this.violations.add(violation);
        if (maxViolations != null && violations.size() >= maxViolations) {
            this.status = Status.TERMINATED;
        }
    }

    public void cancel() {
        if (status == Status.CANCELLED) {
            return;
        }
        if (status == Status.COMPLETED) {
            throw new IllegalStateException("Cannot cancel completed session");
        }
        this.status = Status.CANCELLED;
        if (this.actualEnd == null) {
            this.actualEnd = LocalDateTime.now();
        }
    }

    public boolean isActive() {
        return status == Status.ACTIVE;
    }

    public boolean isTerminated() {
        return status == Status.TERMINATED || status == Status.CANCELLED || status == Status.COMPLETED;
    }

    public boolean hasViolations() {
        return !violations.isEmpty();
    }

    public double getViolationConfidence() {
        return violations.stream()
            .mapToDouble(Violation::getConfidence)
            .average()
            .orElse(0.0);
    }

    public void addParticipant(User participant) {
        if (!participants.contains(participant)) {
            participants.add(participant);
        }
    }

    public void removeParticipant(User participant) {
        participants.remove(participant);
    }

    public boolean isGroupSession() {
        return participants.size() > 1 || groupName != null;
    }

    public enum Status {
        CREATED,
        ACTIVE,
        COMPLETED,
        TERMINATED,
        CANCELLED
    }
}
