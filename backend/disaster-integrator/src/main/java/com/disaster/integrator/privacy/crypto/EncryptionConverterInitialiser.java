package com.disaster.integrator.privacy.crypto;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Publishes the Spring-managed {@link FieldEncryptionService} to the JPA converter,
 * which Hibernate instantiates outside the application context.
 */
@Component
@RequiredArgsConstructor
public class EncryptionConverterInitialiser {

    private final FieldEncryptionService encryptionService;

    @PostConstruct
    void publish() {
        EncryptedStringConverter.setEncryptionService(encryptionService);
    }
}
