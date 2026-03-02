package com.disaster.session.model;

/**
 * Multi-factor authentication type enumeration.
 *
 * @author DisasterResilienceHub Team
 * @version 1.0.0
 */
public enum MfaType {
    /**
     * Time-based One-Time Password (Google Authenticator, Authy, etc.)
     */
    TOTP,

    /**
     * SMS-based verification code
     */
    SMS,

    /**
     * Email-based verification code
     */
    EMAIL,

    /**
     * Authenticator app (generic)
     */
    AUTHENTICATOR_APP
}
