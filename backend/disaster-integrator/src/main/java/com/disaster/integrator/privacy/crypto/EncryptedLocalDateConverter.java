package com.disaster.integrator.privacy.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Encrypts a {@link LocalDate} column, storing it as ciphertext over its ISO-8601 text.
 *
 * <p>Used for date of birth, which is a strong re-identification key even on its own.
 * The column type becomes textual rather than DATE, so date arithmetic must happen in
 * Java rather than SQL -- an acceptable trade for not holding DOB in the clear.
 */
@Converter
public class EncryptedLocalDateConverter implements AttributeConverter<LocalDate, String> {

    private final EncryptedStringConverter delegate = new EncryptedStringConverter();

    @Override
    public String convertToDatabaseColumn(LocalDate attribute) {
        return attribute == null ? null : delegate.convertToDatabaseColumn(attribute.toString());
    }

    @Override
    public LocalDate convertToEntityAttribute(String dbData) {
        String plaintext = delegate.convertToEntityAttribute(dbData);
        if (plaintext == null || plaintext.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(plaintext);
        } catch (DateTimeParseException e) {
            throw new IllegalStateException("Stored date of birth is not a valid ISO-8601 date", e);
        }
    }
}
