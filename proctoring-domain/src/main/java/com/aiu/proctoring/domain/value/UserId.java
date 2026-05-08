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
 * Immutable User identifier value object.
 */
@Embeddable
@Getter
@EqualsAndHashCode
@ToString
public class UserId implements Serializable {

    @Convert(converter = UuidStringConverter.class)
    @Column(name = "id", columnDefinition = "UUID")
    private final String value;

    protected UserId() {
        this.value = null;
    }

    private UserId(String value) {
        this.value = value;
    }

    public static UserId generate() {
        return new UserId(UUID.randomUUID().toString());
    }

    public static UserId from(String value) {
        return new UserId(value);
    }

    public boolean isNil() {
        return value == null || value.isEmpty();
    }
}
