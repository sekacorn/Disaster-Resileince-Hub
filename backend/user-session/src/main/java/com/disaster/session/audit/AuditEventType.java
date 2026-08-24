package com.disaster.session.audit;

/**
 * The security-relevant events this system records.
 *
 * <p>NIST SP 800-53 AU-2 requires an organisation to decide what is worth auditing and
 * to say so explicitly. This enum is that decision, in code rather than in a document
 * that can drift away from what the system actually records.
 *
 * <p>The selection covers the events an investigator needs to answer "who got in, when,
 * and what changed": authentication outcomes, credential and MFA changes, privilege
 * changes, and exercises of data subject rights.
 */
public enum AuditEventType {

    // --- Authentication (AU-2, AC-7) ---
    LOGIN_SUCCEEDED("Authentication succeeded", Severity.INFO),
    LOGIN_FAILED("Authentication failed", Severity.WARNING),
    LOGOUT("Session ended by user", Severity.INFO),
    SESSION_EXPIRED("Session expired", Severity.INFO),

    /** Repeated failures crossed the lockout threshold (AC-7). */
    ACCOUNT_LOCKED("Account locked after repeated failures", Severity.CRITICAL),
    ACCOUNT_UNLOCKED("Account unlocked", Severity.WARNING),

    // --- Credentials and MFA (IA-5) ---
    PASSWORD_CHANGED("Password changed", Severity.WARNING),
    PASSWORD_RESET_REQUESTED("Password reset requested", Severity.WARNING),
    MFA_ENABLED("Multi-factor authentication enabled", Severity.INFO),
    MFA_DISABLED("Multi-factor authentication disabled", Severity.CRITICAL),
    MFA_CHALLENGE_FAILED("Multi-factor challenge failed", Severity.WARNING),

    // --- Accounts and privilege (AC-2, AC-6) ---
    ACCOUNT_CREATED("Account created", Severity.INFO),
    ACCOUNT_DEACTIVATED("Account deactivated", Severity.WARNING),
    ROLE_CHANGED("Role changed", Severity.CRITICAL),

    /** An authenticated caller was refused by an access control decision (AC-3). */
    AUTHORIZATION_DENIED("Authorization denied", Severity.WARNING),

    // --- Data subject rights (GDPR Ch. III, and AU-2 for the access itself) ---
    DATA_EXPORTED("Personal data exported by the subject", Severity.WARNING),
    DATA_ERASED("Personal data erased", Severity.CRITICAL),
    CONSENT_CHANGED("Consent decision recorded", Severity.INFO),

    /** Someone other than the subject read their special category data. */
    SPECIAL_CATEGORY_DATA_ACCESSED("Health data accessed", Severity.WARNING);

    private final String description;
    private final Severity severity;

    AuditEventType(String description, Severity severity) {
        this.description = description;
        this.severity = severity;
    }

    public String getDescription() {
        return description;
    }

    public Severity getSeverity() {
        return severity;
    }

    /** How much attention an event warrants when reviewing the trail (AU-6). */
    public enum Severity {
        INFO,
        WARNING,
        CRITICAL
    }
}
