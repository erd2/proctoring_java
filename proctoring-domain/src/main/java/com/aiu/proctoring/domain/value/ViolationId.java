package com.aiu.proctoring.domain.value;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.util.UUID;

/**
 * Immutable Violation identifier value object.
 */
@Embeddable
@Getter
@EqualsAndHashCode
@ToString
public class ViolationId implements Serializable {

    @Convert(converter = UuidStringConverter.class)
    @Column(name = "id", columnDefinition = "UUID")
    private final String value;

    protected ViolationId() {
        this.value = null;
    }

    private ViolationId(String value) {
        this.value = value;
    }

    public static ViolationId generate() {
        return new ViolationId(UUID.randomUUID().toString());
    }

    public static ViolationId from(String value) {
        return new ViolationId(value);
    }

    public boolean isNil() {
        return value == null || value.isEmpty();
    }
}
