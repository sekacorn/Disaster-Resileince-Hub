package com.disaster.integrator.privacy.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the properties the encryption layer is relied on for, rather than that it
 * merely returns a different string.
 */
class FieldEncryptionServiceTest {

    private static final String CONDITION = "Type 1 diabetes, requires insulin";

    private FieldEncryptionService service;
    private String key;

    @BeforeEach
    void setUp() {
        byte[] raw = new byte[32];
        new SecureRandom().nextBytes(raw);
        key = Base64.getEncoder().encodeToString(raw);
        service = new FieldEncryptionService(key);
        service.initialiseKey();
    }

    @Test
    @DisplayName("A value survives an encrypt/decrypt round trip unchanged")
    void roundTripsValues() {
        assertEquals(CONDITION, service.decrypt(service.encrypt(CONDITION)));
    }

    @Test
    @DisplayName("Ciphertext does not contain the plaintext")
    void ciphertextDoesNotLeakPlaintext() {
        String encrypted = service.encrypt(CONDITION);
        assertFalse(encrypted.contains("diabetes"));
        assertFalse(encrypted.contains("insulin"));
        assertTrue(service.isEncrypted(encrypted));
    }

    @Test
    @DisplayName("Encrypting the same value twice yields different ciphertext")
    void usesAFreshIvEachTime() {
        // A deterministic scheme would let anyone with database access learn which
        // patients share a diagnosis without decrypting anything.
        assertNotEquals(service.encrypt(CONDITION), service.encrypt(CONDITION));
    }

    @Test
    @DisplayName("A tampered ciphertext fails instead of decrypting to something else")
    void rejectsTamperedCiphertext() {
        String encrypted = service.encrypt(CONDITION);
        char[] chars = encrypted.toCharArray();
        int lastIndex = chars.length - 1;
        chars[lastIndex] = chars[lastIndex] == 'A' ? 'B' : 'A';

        assertThrows(IllegalStateException.class, () -> service.decrypt(new String(chars)));
    }

    @Test
    @DisplayName("Null and empty values pass through so nullable columns stay null")
    void passesThroughAbsentValues() {
        assertNull(service.encrypt(null));
        assertNull(service.decrypt(null));
        assertEquals("", service.encrypt(""));
        assertEquals("", service.decrypt(""));
    }

    @Test
    @DisplayName("Values written before encryption existed are still readable")
    void readsLegacyPlaintext() {
        // Rows predating the converter have no version prefix and must not be treated
        // as corrupt ciphertext, or a backfill could never read them.
        assertEquals("legacy plaintext", service.decrypt("legacy plaintext"));
    }

    @Test
    @DisplayName("A second service with the same key can read the first one's output")
    void keyIsTheOnlySharedState() {
        FieldEncryptionService other = new FieldEncryptionService(key);
        other.initialiseKey();
        assertEquals(CONDITION, other.decrypt(service.encrypt(CONDITION)));
    }

    @Test
    @DisplayName("A service with a different key cannot read the ciphertext")
    void differentKeyCannotDecrypt() {
        byte[] raw = new byte[32];
        new SecureRandom().nextBytes(raw);
        FieldEncryptionService other =
                new FieldEncryptionService(Base64.getEncoder().encodeToString(raw));
        other.initialiseKey();

        String encrypted = service.encrypt(CONDITION);
        assertThrows(IllegalStateException.class, () -> other.decrypt(encrypted));
    }

    @Test
    @DisplayName("A key of the wrong length is rejected at startup, not at first write")
    void rejectsUndersizedKey() {
        byte[] tooShort = new byte[16];
        FieldEncryptionService weak =
                new FieldEncryptionService(Base64.getEncoder().encodeToString(tooShort));
        assertThrows(IllegalStateException.class, weak::initialiseKey);
    }

    @Test
    @DisplayName("A missing key still encrypts rather than silently storing plaintext")
    void missingKeyStillEncrypts() {
        FieldEncryptionService ephemeral = new FieldEncryptionService("");
        ephemeral.initialiseKey();

        String encrypted = ephemeral.encrypt(CONDITION);
        assertTrue(ephemeral.isEncrypted(encrypted));
        assertFalse(encrypted.contains("diabetes"));
        assertEquals(CONDITION, ephemeral.decrypt(encrypted));
    }
}
