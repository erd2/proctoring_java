package com.aiu.proctoring.domain.value;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.UUID;

/**
 * Converts between String UUID representation and database UUID column.
 * Applied to @EmbeddedId fields via @Convert on the entity's id field.
 */
@Converter
public class UuidStringConverter implements AttributeConverter<String, UUID> {

    @Override
    public UUID convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(attribute);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID string: '" + attribute + "' (length: " + attribute.length() + ")", e);
        }
    }

    @Override
    public String convertToEntityAttribute(UUID dbData) {
        return dbData == null ? null : dbData.toString();
    }
}
