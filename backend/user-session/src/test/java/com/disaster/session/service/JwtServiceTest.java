package com.disaster.session.service;

import com.disaster.session.model.AccountType;
import com.disaster.session.model.User;
import com.disaster.session.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtService.
 *
 * @author DisasterResilienceHub Team
 * @version 1.0.0
 */
@SpringBootTest
@TestPropertySource(properties = {
    "jwt.secret=c2VjcmV0a2V5Zm9yZGlzYXN0ZXJyZXNpbGllbmNlaHViYXBwbGljYXRpb25zZWN1cml0eXRva2VuZ2VuZXJhdGlvbg==",
    "jwt.expiration=3600000",
    "jwt.refresh-expiration=604800000"
})
class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .role(UserRole.USER)
                .accountType(AccountType.STANDARD)
                .mfaEnabled(false)
                .isActive(true)
                .build();
    }

    @Test
    void testGenerateAccessToken() {
        String token = jwtService.generateAccessToken(testUser);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3); // JWT has 3 parts
    }

    @Test
    void testGenerateRefreshToken() {
        String refreshToken = jwtService.generateRefreshToken(testUser);

        assertNotNull(refreshToken);
        assertFalse(refreshToken.isEmpty());
    }

    @Test
    void testExtractUsername() {
        String token = jwtService.generateAccessToken(testUser);
        String username = jwtService.extractUsername(token);

        assertEquals(testUser.getUsername(), username);
    }

    @Test
    void testExtractUserId() {
        String token = jwtService.generateAccessToken(testUser);
        UUID userId = jwtService.extractUserId(token);

        assertEquals(testUser.getId(), userId);
    }

    @Test
    void testExtractRole() {
        String token = jwtService.generateAccessToken(testUser);
        String role = jwtService.extractRole(token);

        assertEquals(UserRole.USER.name(), role);
    }

    @Test
    void testValidateToken() {
        String token = jwtService.generateAccessToken(testUser);
        Boolean isValid = jwtService.validateToken(token, testUser);

        assertTrue(isValid);
    }

    @Test
    void testValidateTokenWithWrongUser() {
        String token = jwtService.generateAccessToken(testUser);

        User wrongUser = User.builder()
                .id(UUID.randomUUID())
                .username("wronguser")
                .email("wrong@example.com")
                .build();

        Boolean isValid = jwtService.validateToken(token, wrongUser);

        assertFalse(isValid);
    }

    @Test
    void testIsTokenExpired() {
        String token = jwtService.generateAccessToken(testUser);
        Boolean isExpired = jwtService.isTokenExpired(token);

        assertFalse(isExpired);
    }

    @Test
    void testGenerateMfaToken() {
        String mfaToken = jwtService.generateMfaToken(testUser);

        assertNotNull(mfaToken);
        assertFalse(mfaToken.isEmpty());
    }
}
