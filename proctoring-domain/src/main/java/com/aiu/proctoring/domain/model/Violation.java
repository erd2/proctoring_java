package com.aiu.proctoring.domain.model;

import com.aiu.proctoring.domain.value.ViolationId;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

    @EmbeddedId
    private ViolationId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ExamSession session;

    @Column(name = "violation_type", nullable = false, length = 50)
    private String type;

    @Column(nullable = false)
    private Double confidence;

    @Column(name = "frame_timestamp", nullable = false)
    private Long frameTimestamp;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;

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
        detectedAt = LocalDateTime.now();
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
