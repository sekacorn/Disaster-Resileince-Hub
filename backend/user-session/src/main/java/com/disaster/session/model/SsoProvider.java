package com.disaster.session.model;

import jakarta.persistence.*;
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
 * SSO Provider entity for managing enterprise single sign-on configurations.
 *
 * Supports SAML 2.0, OAuth2, and OIDC protocols for seamless integration
 * with corporate identity providers.
 *
 * @author DisasterResilienceHub Team
 * @version 1.0.0
 */
@Entity
@Table(name = "sso_providers")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SsoProvider {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank(message = "Provider name is required")
    @Size(max = 100)
    @Column(name = "provider_name", unique = true, nullable = false)
    private String providerName;

    @NotBlank(message = "Provider type is required")
    @Size(max = 50)
    @Column(name = "provider_type", nullable = false)
    private String providerType; // 'SAML', 'OAUTH2', 'OIDC'

    // SAML Configuration
    @Size(max = 500)
    @Column(name = "metadata_url")
    private String metadataUrl;

    @Size(max = 255)
    @Column(name = "entity_id")
    private String entityId;

    @Size(max = 500)
    @Column(name = "sso_url")
    private String ssoUrl;

    @Column(name = "certificate", columnDefinition = "text")
    private String certificate;

    // OAuth2/OIDC Configuration
    @Size(max = 255)
    @Column(name = "client_id")
    private String clientId;

    @Size(max = 255)
    @Column(name = "client_secret")
    private String clientSecret;

    @Size(max = 500)
    @Column(name = "authorization_endpoint")
    private String authorizationEndpoint;

    @Size(max = 500)
    @Column(name = "token_endpoint")
    private String tokenEndpoint;

    @Size(max = 500)
    @Column(name = "user_info_endpoint")
    private String userInfoEndpoint;

    // Settings
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "auto_provision_users")
    @Builder.Default
    private Boolean autoProvisionUsers = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_role")
    @Builder.Default
    private UserRole defaultRole = UserRole.USER;

    // Timestamps
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Checks if the provider is SAML-based.
     *
     * @return true if provider type is SAML
     */
    public boolean isSamlProvider() {
        return "SAML".equalsIgnoreCase(providerType);
    }

    /**
     * Checks if the provider is OAuth2-based.
     *
     * @return true if provider type is OAUTH2 or OIDC
     */
    public boolean isOAuthProvider() {
        return "OAUTH2".equalsIgnoreCase(providerType) || "OIDC".equalsIgnoreCase(providerType);
    }
}
