package com.disaster.session.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User entity representing a user account in the system.
 *
 * Supports:
 * - Standard username/password authentication
 * - SSO integration (SAML, OAuth2, OIDC)
 * - Multi-factor authentication
 * - Role-based access control
 * - Account locking and security features
 *
 * @author DisasterResilienceHub Team
 * @version 1.0.0
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email", columnList = "email"),
    @Index(name = "idx_users_username", columnList = "username"),
    @Index(name = "idx_users_sso_subject", columnList = "ssoSubjectId"),
    @Index(name = "idx_users_role", columnList = "role")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 255, message = "Username must be between 3 and 255 characters")
    @Column(name = "username", unique = true, nullable = false)
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255)
    @Column(name = "email", unique = true, nullable = false)
    private String email;

    /**
     * BCrypt hashed password. NULL for SSO-only users.
     */
    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    @Builder.Default
    private AccountType accountType = AccountType.STANDARD;

    // Profile information
    @Size(max = 100)
    @Column(name = "first_name")
    private String firstName;

    @Size(max = 100)
    @Column(name = "last_name")
    private String lastName;

    @Size(max = 20)
    @Column(name = "phone_number")
    private String phoneNumber;

    @Size(max = 255)
    @Column(name = "organization")
    private String organization;

    /**
     * MBTI personality type for tailored UX (e.g., "INTJ", "ESFP")
     */
    @Size(max = 4)
    @Column(name = "mbti_type")
    private String mbtiType;

    // SSO fields
    /**
     * SSO provider type: 'SAML', 'OAUTH2', 'OIDC', or NULL for standard auth
     */
    @Size(max = 50)
    @Column(name = "sso_provider")
    private String ssoProvider;

    /**
     * Unique identifier from the SSO provider
     */
    @Size(max = 255)
    @Column(name = "sso_subject_id")
    private String ssoSubjectId;

    /**
     * Additional SSO attributes stored as JSON
     */
    @Column(name = "sso_metadata", columnDefinition = "jsonb")
    private String ssoMetadata;

    // MFA fields
    @Column(name = "mfa_enabled")
    @Builder.Default
    private Boolean mfaEnabled = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "mfa_type")
    private MfaType mfaType;

    /**
     * Encrypted TOTP secret or other MFA data
     */
    @Size(max = 255)
    @Column(name = "mfa_secret")
    private String mfaSecret;

    /**
     * Encrypted backup codes for account recovery
     */
    @Column(name = "mfa_backup_codes", columnDefinition = "text[]")
    private String[] mfaBackupCodes;

    // Account status
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_email_verified")
    @Builder.Default
    private Boolean isEmailVerified = false;

    @Column(name = "is_locked")
    @Builder.Default
    private Boolean isLocked = false;

    @Column(name = "failed_login_attempts")
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    // Timestamps
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    // Audit fields
    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    /**
     * Checks if the account is currently locked.
     *
     * @return true if the account is locked and the lock period hasn't expired
     */
    public boolean isAccountLocked() {
        if (!isLocked) {
            return false;
        }
        if (lockedUntil != null && LocalDateTime.now().isAfter(lockedUntil)) {
            return false;
        }
        return true;
    }

    /**
     * Increments the failed login attempts counter.
     */
    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts = (this.failedLoginAttempts == null ? 0 : this.failedLoginAttempts) + 1;
    }

    /**
     * Resets the failed login attempts counter.
     */
    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.isLocked = false;
        this.lockedUntil = null;
    }

    /**
     * Gets the user's full name.
     *
     * @return the full name or username if names are not set
     */
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        }
        return username;
    }
}
