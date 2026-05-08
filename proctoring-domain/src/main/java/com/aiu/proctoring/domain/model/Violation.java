package com.aiu.proctoring.domain.model;

import com.aiu.proctoring.domain.value.ExamSessionId;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Violation entity representing AI-detected academic integrity breaches.
 */
@Entity
@Table(name = "violations")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Violation {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ExamSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "violation_type", nullable = false, length = 50)
    private String type;

    @Column(nullable = false)
    private Double confidence;

    @Column(name = "frame_timestamp", nullable = false)
    private Long frameTimestamp;

    @Column(name = "description", length = 500)
    private String description;

    @Builder.Default
    @Column(name = "detected_at", nullable = false, updatable = false)
    private LocalDateTime detectedAt = LocalDateTime.now();

    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "is_false_positive")
    private Boolean isFalsePositive;

    @Column(name = "notes", length = 500)
    private String notes;

    @PrePersist
    protected void onCreate() {
        if (detectedAt == null) {
            detectedAt = LocalDateTime.now();
        }
    }

    public void review(String reviewer, boolean falsePositive, String notes) {
        this.reviewedBy = reviewer;
        this.reviewedAt = LocalDateTime.now();
        this.isFalsePositive = falsePositive;
        this.notes = notes;
    }

    public boolean isReviewed() {
        return reviewedAt != null;
    }
}
