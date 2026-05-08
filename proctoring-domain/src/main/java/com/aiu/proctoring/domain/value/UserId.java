package com.aiu.proctoring.domain.value;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable User identifier value object.
 */
@Embeddable
@EqualsAndHashCode
@ToString
public class UserId implements Serializable {

    @Column(name = "id", nullable = false)
    private UUID value;

    protected UserId() {
        this.value = null;
    }

    private UserId(UUID value) {
        this.value = Objects.requireNonNull(value, "UserId value cannot be null");
    }

    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }

    public static UserId from(String value) {
        return new UserId(UUID.fromString(value));
    }

    public String getValue() {
        return value != null ? value.toString() : null;
    }

    public boolean isNil() {
        return value == null;
    }
}
