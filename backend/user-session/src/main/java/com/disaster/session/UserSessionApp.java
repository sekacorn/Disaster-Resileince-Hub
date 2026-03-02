package com.disaster.session;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot application for the User Session microservice.
 *
 * This service handles:
 * - User authentication and authorization
 * - JWT token generation and validation
 * - Role-based access control (USER, MODERATOR, ADMIN)
 * - Session management with Redis
 * - SSO integration (SAML 2.0, OAuth2/OIDC)
 * - Multi-factor authentication (TOTP, SMS, Email)
 * - User management and profile operations
 *
 * @author DisasterResilienceHub Team
 * @version 1.0.0
 * @since 2024-01-20
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableCaching
@EnableAsync
@EnableScheduling
public class UserSessionApp {

    public static void main(String[] args) {
        SpringApplication.run(UserSessionApp.class, args);
    }
}
