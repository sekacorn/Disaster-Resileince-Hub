package com.disaster.gateway.filter;

import com.disaster.gateway.service.JwtValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * JWT Authentication Filter for API Gateway
 *
 * Validates JWT tokens for all protected routes and extracts user information.
 * Passes user details to downstream services via headers.
 *
 * @author Disaster Resilience Hub Team
 */
@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final JwtValidationService jwtValidationService;

    // Public endpoints that don't require authentication
    private static final List<String> PUBLIC_ENDPOINTS = Arrays.asList(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/actuator/health"
    );

    public JwtAuthenticationFilter(JwtValidationService jwtValidationService) {
        super(Config.class);
        this.jwtValidationService = jwtValidationService;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getPath().value();

            // Skip authentication for public endpoints
            if (isPublicEndpoint(path)) {
                return chain.filter(exchange);
            }

            // Extract JWT token from Authorization header
            String authHeader = request.getHeaders().getFirst(AUTHORIZATION_HEADER);

            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                logger.warn("Missing or invalid Authorization header for path: {}", path);
                return onError(exchange, "Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(BEARER_PREFIX.length());

            // Validate token
            return jwtValidationService.validateToken(token)
                    .flatMap(isValid -> {
                        if (!isValid) {
                            logger.warn("Invalid JWT token for path: {}", path);
                            return onError(exchange, "Invalid or expired JWT token", HttpStatus.UNAUTHORIZED);
                        }

                        // Extract user information from token
                        return jwtValidationService.extractUserId(token)
                                .flatMap(userId -> jwtValidationService.extractUsername(token)
                                        .flatMap(username -> jwtValidationService.extractRoles(token)
                                                .flatMap(roles -> {
                                                    // Add user information to request headers for downstream services
                                                    ServerHttpRequest modifiedRequest = exchange.getRequest()
                                                            .mutate()
                                                            .header("X-User-Id", userId)
                                                            .header("X-Username", username)
                                                            .header("X-User-Roles", String.join(",", roles))
                                                            .build();

                                                    ServerWebExchange modifiedExchange = exchange.mutate()
                                                            .request(modifiedRequest)
                                                            .build();

                                                    logger.debug("Successfully authenticated user: {} for path: {}", username, path);
                                                    return chain.filter(modifiedExchange);
                                                })))
                                .onErrorResume(e -> {
                                    logger.error("Error extracting user information from token: {}", e.getMessage());
                                    return onError(exchange, "Error processing JWT token", HttpStatus.UNAUTHORIZED);
                                });
                    })
                    .onErrorResume(e -> {
                        logger.error("Error validating JWT token: {}", e.getMessage());
                        return onError(exchange, "Authentication failed", HttpStatus.UNAUTHORIZED);
                    });
        };
    }

    /**
     * Check if the path is a public endpoint
     */
    private boolean isPublicEndpoint(String path) {
        return PUBLIC_ENDPOINTS.stream().anyMatch(path::startsWith);
    }

    /**
     * Handle authentication errors
     */
    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");

        String errorResponse = String.format("{\"error\":\"%s\",\"status\":%d}",
                message, httpStatus.value());

        return response.writeWith(Mono.just(response.bufferFactory()
                .wrap(errorResponse.getBytes())));
    }

    /**
     * Configuration class for JWT Authentication Filter
     */
    public static class Config {
        // Configuration properties can be added here if needed
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
