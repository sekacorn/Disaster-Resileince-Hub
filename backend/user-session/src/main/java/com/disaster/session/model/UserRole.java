package com.disaster.session.model;

/**
 * User role enumeration for role-based access control (RBAC).
 *
 * Roles define the level of access and permissions a user has within the system:
 * - USER: Standard user with basic access to features
 * - MODERATOR: Elevated permissions for content moderation and user management
 * - ADMIN: Full system access with administrative capabilities
 *
 * @author DisasterResilienceHub Team
 * @version 1.0.0
 */
public enum UserRole {
    /**
     * Standard user role with basic feature access
     */
    USER,

    /**
     * Moderator role with elevated permissions for content moderation
     */
    MODERATOR,

    /**
     * Administrator role with full system access
     */
    ADMIN
}
