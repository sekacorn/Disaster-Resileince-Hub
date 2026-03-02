package com.disaster.gateway;

import com.disaster.gateway.service.JwtValidationService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for JWT Validation Service
 *
 * @author Disaster Resilience Hub Team
 */
@SpringBootTest
@TestPropertySource(properties = {
        "jwt.secret=YourSuperSecretKeyForJWTTokenGenerationAndValidationMustBeLongEnough"
})
class JwtValidationServiceTest {

    @Autowired
    private JwtValidationService jwtValidationService;

    private SecretKey secretKey;
    private String validToken;
    private String expiredToken;

    @BeforeEach
    void setUp() {
        String secret = "YourSuperSecretKeyForJWTTokenGenerationAndValidationMustBeLongEnough";
        secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        // Create valid token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", "123");
        claims.put("roles", Arrays.asList("USER", "ADMIN"));
        claims.put("email", "test@example.com");

        validToken = Jwts.builder()
                .subject("testuser")
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000)) // 24 hours
                .signWith(secretKey)
                .compact();

        // Create expired token
        expiredToken = Jwts.builder()
                .subject("testuser")
                .claims(claims)
                .issuedAt(new Date(System.currentTimeMillis() - 172800000)) // 2 days ago
                .expiration(new Date(System.currentTimeMillis() - 86400000)) // expired 1 day ago
                .signWith(secretKey)
                .compact();
    }

    @Test
    void testValidateValidToken() {
        StepVerifier.create(jwtValidationService.validateToken(validToken))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void testValidateExpiredToken() {
        StepVerifier.create(jwtValidationService.validateToken(expiredToken))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void testValidateInvalidToken() {
        StepVerifier.create(jwtValidationService.validateToken("invalid.token.here"))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void testExtractUserId() {
        StepVerifier.create(jwtValidationService.extractUserId(validToken))
                .expectNext("123")
                .verifyComplete();
    }

    @Test
    void testExtractUsername() {
        StepVerifier.create(jwtValidationService.extractUsername(validToken))
                .expectNext("testuser")
                .verifyComplete();
    }

    @Test
    void testExtractRoles() {
        StepVerifier.create(jwtValidationService.extractRoles(validToken))
                .expectNextMatches(roles -> roles.contains("USER") && roles.contains("ADMIN"))
                .verifyComplete();
    }

    @Test
    void testExtractEmail() {
        StepVerifier.create(jwtValidationService.extractEmail(validToken))
                .expectNext("test@example.com")
                .verifyComplete();
    }

    @Test
    void testIsTokenExpired() {
        StepVerifier.create(jwtValidationService.isTokenExpired(validToken))
                .expectNext(false)
                .verifyComplete();

        StepVerifier.create(jwtValidationService.isTokenExpired(expiredToken))
                .expectNext(true)
                .verifyComplete();
    }
}
