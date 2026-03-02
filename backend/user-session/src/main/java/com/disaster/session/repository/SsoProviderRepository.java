package com.disaster.session.repository;

import com.disaster.session.model.SsoProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for SsoProvider entity operations.
 *
 * Manages SSO provider configurations for enterprise authentication.
 *
 * @author DisasterResilienceHub Team
 * @version 1.0.0
 */
@Repository
public interface SsoProviderRepository extends JpaRepository<SsoProvider, UUID> {

    /**
     * Finds an SSO provider by name.
     *
     * @param providerName the provider name
     * @return an Optional containing the provider if found
     */
    Optional<SsoProvider> findByProviderName(String providerName);

    /**
     * Finds all active SSO providers.
     *
     * @return list of active providers
     */
    List<SsoProvider> findByIsActiveTrue();

    /**
     * Finds SSO providers by type.
     *
     * @param providerType the provider type (SAML, OAUTH2, OIDC)
     * @return list of providers with the specified type
     */
    List<SsoProvider> findByProviderType(String providerType);

    /**
     * Checks if a provider name exists.
     *
     * @param providerName the provider name
     * @return true if the provider exists
     */
    boolean existsByProviderName(String providerName);
}
