package com.disaster.integrator.privacy.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Transparently encrypts a String column on write and decrypts it on read.
 *
 * <p>Apply with {@code @Convert(converter = EncryptedStringConverter.class)} to any
 * field holding personal or special category data. Encryption happens below the
 * service layer, so no caller can accidentally bypass it.
 *
 * <p>Hibernate instantiates converters outside the Spring context, so the delegate
 * is supplied statically by {@link EncryptionConverterInitialiser} during startup
 * rather than injected. If a converter runs before that has happened the call fails
 * loudly -- writing plaintext would defeat the point of the converter.
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static volatile FieldEncryptionService encryptionService;

    static void setEncryptionService(FieldEncryptionService service) {
        encryptionService = service;
    }

    private static FieldEncryptionService require() {
        FieldEncryptionService service = encryptionService;
        if (service == null) {
            throw new IllegalStateException(
                    "EncryptedStringConverter used before FieldEncryptionService was published. "
                            + "Ensure EncryptionConverterInitialiser is component-scanned.");
        }
        return service;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return require().encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return require().decrypt(dbData);
    }
}
