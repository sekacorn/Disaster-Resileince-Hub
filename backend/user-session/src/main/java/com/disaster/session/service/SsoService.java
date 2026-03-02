package com.disaster.session.service;

import com.disaster.session.model.AccountType;
import com.disaster.session.model.SsoProvider;
import com.disaster.session.model.User;
import com.disaster.session.repository.SsoProviderRepository;
import com.disaster.session.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for SSO (Single Sign-On) operations.
 *
 * Supports:
 * - SAML 2.0 authentication
 * - OAuth2/OIDC authentication
 * - Auto-provisioning of users from SSO
 * - SSO provider configuration management
 *
 * @author DisasterResilienceHub Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SsoService {

    private final SsoProviderRepository ssoProviderRepository;
    private final UserRepository userRepository;

    /**
     * Creates a new SSO provider configuration.
     *
     * @param provider the SSO provider
     * @return the created provider
     */
    @Transactional
    public SsoProvider createProvider(SsoProvider provider) {
        if (ssoProviderRepository.existsByProviderName(provider.getProviderName())) {
            throw new RuntimeException("Provider with this name already exists");
        }

        provider = ssoProviderRepository.save(provider);
        log.info("SSO provider created: {}", provider.getProviderName());

        return provider;
    }

    /**
     * Gets an SSO provider by ID.
     *
     * @param providerId the provider ID
     * @return the SSO provider
     */
    @Transactional(readOnly = true)
    public SsoProvider getProviderById(UUID providerId) {
        return ssoProviderRepository.findById(providerId)
                .orElseThrow(() -> new RuntimeException("SSO provider not found"));
    }

    /**
     * Gets an SSO provider by name.
     *
     * @param providerName the provider name
     * @return the SSO provider
     */
    @Transactional(readOnly = true)
    public SsoProvider getProviderByName(String providerName) {
        return ssoProviderRepository.findByProviderName(providerName)
                .orElseThrow(() -> new RuntimeException("SSO provider not found"));
    }

    /**
     * Gets all active SSO providers.
     *
     * @return the list of active providers
     */
    @Transactional(readOnly = true)
    public List<SsoProvider> getActiveProviders() {
        return ssoProviderRepository.findByIsActiveTrue();
    }

    /**
     * Updates an SSO provider configuration.
     *
     * @param providerId the provider ID
     * @param provider the updated provider data
     * @return the updated provider
     */
    @Transactional
    public SsoProvider updateProvider(UUID providerId, SsoProvider provider) {
        SsoProvider existing = ssoProviderRepository.findById(providerId)
                .orElseThrow(() -> new RuntimeException("SSO provider not found"));

        // Update fields
        existing.setProviderType(provider.getProviderType());
        existing.setMetadataUrl(provider.getMetadataUrl());
        existing.setEntityId(provider.getEntityId());
        existing.setSsoUrl(provider.getSsoUrl());
        existing.setCertificate(provider.getCertificate());
        existing.setClientId(provider.getClientId());
        existing.setClientSecret(provider.getClientSecret());
        existing.setAuthorizationEndpoint(provider.getAuthorizationEndpoint());
        existing.setTokenEndpoint(provider.getTokenEndpoint());
        existing.setUserInfoEndpoint(provider.getUserInfoEndpoint());
        existing.setIsActive(provider.getIsActive());
        existing.setAutoProvisionUsers(provider.getAutoProvisionUsers());
        existing.setDefaultRole(provider.getDefaultRole());

        existing = ssoProviderRepository.save(existing);
        log.info("SSO provider updated: {}", existing.getProviderName());

        return existing;
    }

    /**
     * Deletes an SSO provider.
     *
     * @param providerId the provider ID
     */
    @Transactional
    public void deleteProvider(UUID providerId) {
        SsoProvider provider = ssoProviderRepository.findById(providerId)
                .orElseThrow(() -> new RuntimeException("SSO provider not found"));

        ssoProviderRepository.delete(provider);
        log.info("SSO provider deleted: {}", provider.getProviderName());
    }

    /**
     * Authenticates a user via SSO and auto-provisions if enabled.
     *
     * @param providerName the SSO provider name
     * @param ssoSubjectId the unique identifier from SSO provider
     * @param attributes the user attributes from SSO
     * @return the authenticated/provisioned user
     */
    @Transactional
    public User authenticateViaSso(String providerName, String ssoSubjectId, Map<String, Object> attributes) {
        SsoProvider provider = getProviderByName(providerName);

        if (!provider.getIsActive()) {
            throw new RuntimeException("SSO provider is not active");
        }

        // Check if user exists
        User user = userRepository.findBySsoProviderAndSsoSubjectId(providerName, ssoSubjectId)
                .orElse(null);

        if (user == null) {
            if (!provider.getAutoProvisionUsers()) {
                throw new RuntimeException("User not found and auto-provisioning is disabled");
            }

            // Auto-provision user
            user = autoProvisionUser(provider, ssoSubjectId, attributes);
        } else {
            // Update SSO metadata
            user.setSsoMetadata(convertAttributesToJson(attributes));
            user = userRepository.save(user);
        }

        log.info("SSO authentication successful for user: {} via provider: {}", user.getUsername(), providerName);

        return user;
    }

    /**
     * Auto-provisions a new user from SSO attributes.
     *
     * @param provider the SSO provider
     * @param ssoSubjectId the SSO subject ID
     * @param attributes the user attributes
     * @return the provisioned user
     */
    private User autoProvisionUser(SsoProvider provider, String ssoSubjectId, Map<String, Object> attributes) {
        String email = (String) attributes.get("email");
        String username = (String) attributes.getOrDefault("username", email);
        String firstName = (String) attributes.get("firstName");
        String lastName = (String) attributes.get("lastName");

        if (email == null) {
            throw new RuntimeException("Email is required for auto-provisioning");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("User with this email already exists");
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .role(provider.getDefaultRole())
                .accountType(AccountType.ENTERPRISE)
                .ssoProvider(provider.getProviderName())
                .ssoSubjectId(ssoSubjectId)
                .ssoMetadata(convertAttributesToJson(attributes))
                .isActive(true)
                .isEmailVerified(true) // SSO users are pre-verified
                .mfaEnabled(false)
                .build();

        user = userRepository.save(user);
        log.info("User auto-provisioned from SSO: {}", user.getUsername());

        return user;
    }

    /**
     * Converts SSO attributes to JSON string.
     *
     * @param attributes the attributes map
     * @return the JSON string
     */
    private String convertAttributesToJson(Map<String, Object> attributes) {
        // In production, use a proper JSON library like Jackson
        // For now, return a simple string representation
        return attributes.toString();
    }

    /**
     * Handles SAML 2.0 authentication.
     *
     * @param providerName the provider name
     * @param samlResponse the SAML response
     * @return the authenticated user
     */
    @Transactional
    public User handleSamlAuthentication(String providerName, String samlResponse) {
        // TODO: Implement SAML response parsing and validation
        // This is a placeholder for SAML 2.0 implementation

        log.info("SAML authentication initiated for provider: {}", providerName);

        throw new UnsupportedOperationException("SAML authentication not yet fully implemented");
    }

    /**
     * Handles OAuth2/OIDC authentication.
     *
     * @param providerName the provider name
     * @param authorizationCode the authorization code
     * @return the authenticated user
     */
    @Transactional
    public User handleOAuthAuthentication(String providerName, String authorizationCode) {
        // TODO: Implement OAuth2 token exchange and user info retrieval
        // This is a placeholder for OAuth2/OIDC implementation

        log.info("OAuth authentication initiated for provider: {}", providerName);

        throw new UnsupportedOperationException("OAuth authentication not yet fully implemented");
    }
}
