package com.disaster.gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter.Mode;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Security Configuration for API Gateway
 *
 * Configures:
 * - CORS settings for cross-origin requests
 * - CSRF protection
 * - Security headers
 * - Public and protected endpoints
 *
 * @author Disaster Resilience Hub Team
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /**
     * Configure security filter chain
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                // CORS configuration
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // CSRF disabled for stateless API
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                // Authorize requests
                .authorizeExchange(exchanges -> exchanges
                        // Public endpoints
                        .pathMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()
                        .pathMatchers("/api/v1/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/actuator/health/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/actuator/info").permitAll()

                        // All other requests require authentication
                        .anyExchange().authenticated()
                )

                /*
                 * Response security headers.
                 *
                 * NIST SP 800-53 SC-8 (transmission confidentiality), SC-18 (mobile
                 * code) and SI-10. Spring already sets nosniff and no-cache defaults;
                 * what follows is what it does not set on its own.
                 */
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.mode(Mode.DENY))
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; " +
                                        "frame-ancestors 'none'; " +
                                        "base-uri 'self'; " +
                                        // Stops a compromised page posting stolen data
                                        // to an attacker-controlled endpoint.
                                        "form-action 'self'; " +
                                        "object-src 'none'; " +
                                        "upgrade-insecure-requests"))

                        // SC-8(1): tells browsers never to reach this origin over
                        // plaintext again, closing the first-request downgrade window.
                        .hsts(hsts -> hsts
                                .includeSubdomains(true)
                                .maxAge(java.time.Duration.ofDays(365)))

                        /*
                         * Referrer-Policy: a URL can carry an incident id or a record
                         * reference, and the default policy leaks the full URL to any
                         * same-protocol third party the user navigates to.
                         */
                        .referrerPolicy(referrer -> referrer.policy(
                                org.springframework.security.web.server.header
                                        .ReferrerPolicyServerHttpHeadersWriter
                                        .ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))

                        /*
                         * Permissions-Policy: this is an API gateway, so no response it
                         * serves has any business using these capabilities. Denying them
                         * limits what injected script could reach for.
                         */
                        .writer(exchange -> {
                            exchange.getResponse().getHeaders().set("Permissions-Policy",
                                    "geolocation=(), camera=(), microphone=(), "
                                            + "payment=(), usb=(), interest-cohort=()");
                            return reactor.core.publisher.Mono.empty();
                        })
                )

                .build();
    }

    /**
     * Configure CORS
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allowed origins - configure based on environment
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "https://*.disaster-resilience-hub.com",
                "https://disaster-resilience-hub.com"
        ));

        // Allowed HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        // Allowed headers
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));

        // Exposed headers (visible to client)
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "X-RateLimit-Limit",
                "X-RateLimit-Remaining",
                "X-RateLimit-Reset",
                "Retry-After"
        ));

        // Allow credentials
        configuration.setAllowCredentials(true);

        // Max age for preflight requests (1 hour)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
