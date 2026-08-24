package com.disaster.integrator.privacy.crypto;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption for personal data held at rest.
 *
 * <p>Backs the {@link EncryptedStringConverter} JPA converter so that special
 * category data (GDPR Art. 9) is never written to the database in plaintext.
 * GCM is used rather than CBC so that each value carries an authentication tag:
 * a tampered ciphertext fails to decrypt instead of silently yielding garbage.
 *
 * <p>Stored format is {@code v1:<base64(iv || ciphertext || tag)>}. The version
 * prefix lets values written by an older key or algorithm be recognised during a
 * future key rotation rather than being mistaken for plaintext.
 *
 * <p>The key is supplied as base64 via {@code privacy.encryption.key}. In a real
 * deployment this comes from a KMS or secret manager, never from a config file.
 */
@Slf4j
@Service
public class FieldEncryptionService {

    /** Marker identifying the current ciphertext format. */
    static final String VERSION_PREFIX = "v1:";

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final int AES_KEY_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();
    private final String configuredKey;

    private SecretKey key;

    public FieldEncryptionService(@Value("${privacy.encryption.key:}") String configuredKey) {
        this.configuredKey = configuredKey;
    }

    @PostConstruct
    void initialiseKey() {
        if (configuredKey == null || configuredKey.isBlank()) {
            // A generated key makes existing ciphertext unreadable on restart. That is
            // the correct failure mode for a demo, and it is loud rather than silent:
            // the alternative -- defaulting to a hardcoded key -- is indistinguishable
            // from no encryption at all.
            byte[] generated = new byte[AES_KEY_BYTES];
            secureRandom.nextBytes(generated);
            this.key = new SecretKeySpec(generated, "AES");
            log.warn("privacy.encryption.key is not set. Generated an ephemeral key; "
                    + "encrypted personal data will NOT survive a restart. "
                    + "Set privacy.encryption.key (base64, 32 bytes) before storing real data.");
            return;
        }

        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(configuredKey.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("privacy.encryption.key must be valid base64", e);
        }
        if (decoded.length != AES_KEY_BYTES) {
            throw new IllegalStateException(
                    "privacy.encryption.key must decode to " + AES_KEY_BYTES + " bytes for AES-256, got "
                            + decoded.length);
        }
        this.key = new SecretKeySpec(decoded, "AES");
        log.info("Field encryption initialised with a configured AES-256 key.");
    }

    /**
     * Encrypts a value for storage. Null and empty inputs pass through unchanged so
     * that nullable columns stay null and remain queryable as such.
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        try {
            byte[] iv = new byte[GCM_IV_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            return VERSION_PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            // Failing closed matters here: returning the plaintext on error would write
            // unencrypted special category data to the database.
            throw new IllegalStateException("Failed to encrypt personal data field", e);
        }
    }

    /**
     * Decrypts a stored value. Values without the version prefix are returned as-is
     * so that rows written before encryption was introduced remain readable.
     */
    public String decrypt(String stored) {
        if (stored == null || stored.isEmpty()) {
            return stored;
        }
        if (!stored.startsWith(VERSION_PREFIX)) {
            return stored;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(stored.substring(VERSION_PREFIX.length()));
            if (combined.length <= GCM_IV_BYTES) {
                throw new IllegalArgumentException("Ciphertext too short to contain an IV");
            }

            byte[] iv = new byte[GCM_IV_BYTES];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_BYTES);
            byte[] ciphertext = new byte[combined.length - GCM_IV_BYTES];
            System.arraycopy(combined, GCM_IV_BYTES, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt personal data field", e);
        }
    }

    /** True when the value is already stored as ciphertext in the current format. */
    public boolean isEncrypted(String stored) {
        return stored != null && stored.startsWith(VERSION_PREFIX);
    }
}
