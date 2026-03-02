package com.disaster.gateway.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * JWT Validation Service for API Gateway
 *
 * Validates JWT tokens and extracts user information.
 * Uses the same secret key as the authentication service.
 *
 * @author Disaster Resilience Hub Team
 */
@Service
public class JwtValidationService {

    private static final Logger logger = LoggerFactory.getLogger(JwtValidationService.class);

    private final SecretKey secretKey;

    public JwtValidationService(@Value("${jwt.secret}") String jwtSecret) {
        // Create secret key from configured secret
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Validate JWT token
     *
     * @param token JWT token to validate
     * @return Mono<Boolean> indicating if token is valid
     */
    public Mono<Boolean> validateToken(String token) {
        return Mono.fromCallable(() -> {
            try {
                Claims claims = parseToken(token);

                // Check if token is expired
                if (claims.getExpiration().before(new Date())) {
                    logger.warn("JWT token is expired");
                    return false;
                }

                return true;
            } catch (SignatureException e) {
                logger.error("Invalid JWT signature: {}", e.getMessage());
                return false;
            } catch (MalformedJwtException e) {
                logger.error("Invalid JWT token: {}", e.getMessage());
                return false;
            } catch (ExpiredJwtException e) {
                logger.error("JWT token is expired: {}", e.getMessage());
                return false;
            } catch (UnsupportedJwtException e) {
                logger.error("JWT token is unsupported: {}", e.getMessage());
                return false;
            } catch (IllegalArgumentException e) {
                logger.error("JWT claims string is empty: {}", e.getMessage());
                return false;
            } catch (Exception e) {
                logger.error("Error validating JWT token: {}", e.getMessage());
                return false;
            }
        });
    }

    /**
     * Extract user ID from JWT token
     *
     * @param token JWT token
     * @return Mono<String> containing user ID
     */
    public Mono<String> extractUserId(String token) {
        return Mono.fromCallable(() -> {
            try {
                Claims claims = parseToken(token);
                return claims.get("userId", String.class);
            } catch (Exception e) {
                logger.error("Error extracting user ID from token: {}", e.getMessage());
                throw new RuntimeException("Failed to extract user ID", e);
            }
        });
    }

    /**
     * Extract username from JWT token
     *
     * @param token JWT token
     * @return Mono<String> containing username
     */
    public Mono<String> extractUsername(String token) {
        return Mono.fromCallable(() -> {
            try {
                Claims claims = parseToken(token);
                return claims.getSubject();
            } catch (Exception e) {
                logger.error("Error extracting username from token: {}", e.getMessage());
                throw new RuntimeException("Failed to extract username", e);
            }
        });
    }

    /**
     * Extract user roles from JWT token
     *
     * @param token JWT token
     * @return Mono<List<String>> containing user roles
     */
    public Mono<List<String>> extractRoles(String token) {
        return Mono.fromCallable(() -> {
            try {
                Claims claims = parseToken(token);
                @SuppressWarnings("unchecked")
                List<String> roles = claims.get("roles", List.class);
                return roles != null ? roles : List.of();
            } catch (Exception e) {
                logger.error("Error extracting roles from token: {}", e.getMessage());
                throw new RuntimeException("Failed to extract roles", e);
            }
        });
    }

    /**
     * Extract email from JWT token
     *
     * @param token JWT token
     * @return Mono<String> containing email
     */
    public Mono<String> extractEmail(String token) {
        return Mono.fromCallable(() -> {
            try {
                Claims claims = parseToken(token);
                return claims.get("email", String.class);
            } catch (Exception e) {
                logger.error("Error extracting email from token: {}", e.getMessage());
                throw new RuntimeException("Failed to extract email", e);
            }
        });
    }

    /**
     * Check if token is expired
     *
     * @param token JWT token
     * @return Mono<Boolean> indicating if token is expired
     */
    public Mono<Boolean> isTokenExpired(String token) {
        return Mono.fromCallable(() -> {
            try {
                Claims claims = parseToken(token);
                return claims.getExpiration().before(new Date());
            } catch (ExpiredJwtException e) {
                return true;
            } catch (Exception e) {
                logger.error("Error checking token expiration: {}", e.getMessage());
                return true;
            }
        });
    }

    /**
     * Extract all claims from JWT token
     *
     * @param token JWT token
     * @return Mono<Claims> containing all claims
     */
    public Mono<Claims> extractAllClaims(String token) {
        return Mono.fromCallable(() -> {
            try {
                return parseToken(token);
            } catch (Exception e) {
                logger.error("Error extracting claims from token: {}", e.getMessage());
                throw new RuntimeException("Failed to extract claims", e);
            }
        });
    }

    /**
     * Parse JWT token and extract claims
     *
     * @param token JWT token
     * @return Claims object
     */
    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
