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
 * Immutable ExamSession identifier value object.
 */
@Embeddable
@Getter
@EqualsAndHashCode
@ToString
public class ExamSessionId implements Serializable {

    @Convert(converter = UuidStringConverter.class)
    @Column(name = "id", columnDefinition = "UUID")
    private final String value;

    protected ExamSessionId() {
        this.value = null;
    }

    private ExamSessionId(String value) {
        this.value = value;
    }

    public static ExamSessionId generate() {
        return new ExamSessionId(UUID.randomUUID().toString());
    }

    public static ExamSessionId from(String value) {
        return new ExamSessionId(value);
    }

    public boolean isNil() {
        return value == null || value.isEmpty();
    }
}
