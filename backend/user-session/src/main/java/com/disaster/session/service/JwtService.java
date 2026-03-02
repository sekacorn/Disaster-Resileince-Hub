package com.disaster.session.service;

import com.disaster.session.model.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service for JWT token generation, validation, and parsing.
 *
 * Handles:
 * - Access token generation with user claims
 * - Refresh token generation
 * - Token validation and expiration checking
 * - Claims extraction and parsing
 *
 * @author DisasterResilienceHub Team
 * @version 1.0.0
 */
@Service
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:3600000}") // Default 1 hour in milliseconds
    private Long expiration;

    @Value("${jwt.refresh-expiration:604800000}") // Default 7 days in milliseconds
    private Long refreshExpiration;

    /**
     * Generates an access token for a user.
     *
     * @param user the user
     * @return the JWT access token
     */
    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("username", user.getUsername());
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole().name());
        claims.put("accountType", user.getAccountType().name());
        claims.put("mfaEnabled", user.getMfaEnabled());

        return createToken(claims, user.getUsername(), expiration);
    }

    /**
     * Generates a refresh token for a user.
     *
     * @param user the user
     * @return the JWT refresh token
     */
    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("type", "refresh");

        return createToken(claims, user.getUsername(), refreshExpiration);
    }

    /**
     * Generates a temporary MFA token for two-factor authentication.
     *
     * @param user the user
     * @return the temporary MFA token
     */
    public String generateMfaToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("type", "mfa");
        claims.put("mfaRequired", true);

        // MFA tokens expire in 5 minutes
        return createToken(claims, user.getUsername(), 300000L);
    }

    /**
     * Generates a password reset token for a user.
     *
     * @param user the user
     * @return the password reset token
     */
    public String generatePasswordResetToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("type", "password-reset");

        // Reset tokens expire in 1 hour
        return createToken(claims, user.getUsername(), 3600000L);
    }

    /**
     * Creates a JWT token with the specified claims and expiration.
     *
     * @param claims the claims to include
     * @param subject the subject (username)
     * @param expirationTime the expiration time in milliseconds
     * @return the JWT token
     */
    private String createToken(Map<String, Object> claims, String subject, Long expirationTime) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Extracts the username from a token.
     *
     * @param token the JWT token
     * @return the username
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the user ID from a token.
     *
     * @param token the JWT token
     * @return the user ID
     */
    public UUID extractUserId(String token) {
        String userIdStr = extractClaim(token, claims -> claims.get("userId", String.class));
        return UUID.fromString(userIdStr);
    }

    /**
     * Alias for extractUserId - gets user ID from token.
     *
     * @param token the JWT token
     * @return the user ID
     */
    public UUID getUserIdFromToken(String token) {
        return extractUserId(token);
    }

    /**
     * Extracts the expiration date from a token.
     *
     * @param token the JWT token
     * @return the expiration date
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extracts the user role from a token.
     *
     * @param token the JWT token
     * @return the user role
     */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    /**
     * Extracts authorities (roles and permissions) from a token.
     *
     * @param token the JWT token
     * @return the list of granted authorities
     */
    public List<GrantedAuthority> extractAuthorities(String token) {
        String role = extractRole(token);
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
    }

    /**
     * Extracts a specific claim from a token.
     *
     * @param token the JWT token
     * @param claimsResolver the function to resolve the claim
     * @param <T> the type of the claim
     * @return the claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extracts all claims from a token.
     *
     * @param token the JWT token
     * @return the claims
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Checks if a token is expired.
     *
     * @param token the JWT token
     * @return true if the token is expired
     */
    public Boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /**
     * Validates a token against a user.
     *
     * @param token the JWT token
     * @param user the user
     * @return true if the token is valid
     */
    public Boolean validateToken(String token, User user) {
        try {
            final String username = extractUsername(token);
            return (username.equals(user.getUsername()) && !isTokenExpired(token));
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT validation error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validates a token without checking against a user.
     *
     * @param token the JWT token
     * @return true if the token is valid and not expired
     */
    public Boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT validation error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Gets the signing key for JWT operations.
     *
     * @return the secret key
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Gets the token expiration time in seconds.
     *
     * @return the expiration time in seconds
     */
    public Long getExpirationInSeconds() {
        return expiration / 1000;
    }

    /**
     * Gets the refresh token expiration time in seconds.
     *
     * @return the refresh token expiration time in seconds
     */
    public Long getRefreshExpirationInSeconds() {
        return refreshExpiration / 1000;
    }
}
