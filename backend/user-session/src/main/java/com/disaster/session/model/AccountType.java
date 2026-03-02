package com.disaster.session.model;

/**
 * Account type enumeration for different subscription tiers.
 *
 * @author DisasterResilienceHub Team
 * @version 1.0.0
 */
public enum AccountType {
    /**
     * Standard account with basic features
     */
    STANDARD,

    /**
     * Enterprise account with SSO, MFA, and advanced features
     */
    ENTERPRISE,

    /**
     * Non-profit account with special pricing and features
     */
    NONPROFIT
}
