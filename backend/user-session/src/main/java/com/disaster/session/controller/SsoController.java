package com.disaster.session.controller;

import com.disaster.session.model.SsoProvider;
import com.disaster.session.service.SsoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for SSO provider management endpoints.
 *
 * @author DisasterResilienceHub Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/sso")
@RequiredArgsConstructor
@Slf4j
public class SsoController {

    private final SsoService ssoService;

    @PostMapping("/providers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SsoProvider> createProvider(@Valid @RequestBody SsoProvider provider) {
        SsoProvider created = ssoService.createProvider(provider);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/providers/{providerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SsoProvider> getProvider(@PathVariable UUID providerId) {
        SsoProvider provider = ssoService.getProviderById(providerId);
        return ResponseEntity.ok(provider);
    }

    @GetMapping("/providers")
    public ResponseEntity<List<SsoProvider>> getActiveProviders() {
        List<SsoProvider> providers = ssoService.getActiveProviders();
        return ResponseEntity.ok(providers);
    }

    @PutMapping("/providers/{providerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SsoProvider> updateProvider(
            @PathVariable UUID providerId,
            @Valid @RequestBody SsoProvider provider
    ) {
        SsoProvider updated = ssoService.updateProvider(providerId, provider);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/providers/{providerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteProvider(@PathVariable UUID providerId) {
        ssoService.deleteProvider(providerId);
        return ResponseEntity.ok(Map.of("message", "SSO provider deleted"));
    }

    @PostMapping("/auth/{providerName}")
    public ResponseEntity<Map<String, String>> initiateSsoAuth(@PathVariable String providerName) {
        // This would typically redirect to the SSO provider's login page
        log.info("SSO authentication initiated for provider: {}", providerName);
        return ResponseEntity.ok(Map.of("message", "SSO authentication initiated", "provider", providerName));
    }

    @PostMapping("/callback/{providerName}")
    public ResponseEntity<Map<String, String>> handleSsoCallback(
            @PathVariable String providerName,
            @RequestBody Map<String, Object> callbackData
    ) {
        log.info("SSO callback received for provider: {}", providerName);
        // Handle SSO callback and authenticate user
        return ResponseEntity.ok(Map.of("message", "SSO authentication completed"));
    }
}
