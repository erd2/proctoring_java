package com.aiu.proctoring.domain.value;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable ExamSession identifier value object.
 */
@Embeddable
@EqualsAndHashCode
@ToString
public class ExamSessionId implements Serializable {

    @Column(name = "id", nullable = false)
    private UUID value;

    protected ExamSessionId() {
        this.value = null;
    }

    private ExamSessionId(UUID value) {
        this.value = Objects.requireNonNull(value, "ExamSessionId value cannot be null");
    }

    public static ExamSessionId generate() {
        return new ExamSessionId(UUID.randomUUID());
    }

    public static ExamSessionId from(String value) {
        return new ExamSessionId(UUID.fromString(value));
    }

    public String getValue() {
        return value != null ? value.toString() : null;
    }

    public boolean isNil() {
        return value == null;
    }
}
