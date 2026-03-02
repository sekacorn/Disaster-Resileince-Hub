package com.disaster.gateway;

import com.disaster.gateway.service.JwtValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Integration tests for API Gateway
 *
 * Tests routing, authentication, and filter behavior.
 *
 * @author Disaster Resilience Hub Team
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewayIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private JwtValidationService jwtValidationService;

    private String validToken;

    @BeforeEach
    void setUp() {
        validToken = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0dXNlciIsInVzZXJJZCI6IjEyMyIsInJvbGVzIjpbIlVTRVIiXX0.test";

        // Mock JWT validation service
        when(jwtValidationService.validateToken(anyString()))
                .thenReturn(Mono.just(true));
        when(jwtValidationService.extractUserId(anyString()))
                .thenReturn(Mono.just("123"));
        when(jwtValidationService.extractUsername(anyString()))
                .thenReturn(Mono.just("testuser"));
        when(jwtValidationService.extractRoles(anyString()))
                .thenReturn(Mono.just(Arrays.asList("USER")));
    }

    @Test
    void testHealthEndpoint() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.status").exists();
    }

    @Test
    void testHealthAggregationEndpoint() {
        webTestClient.get()
                .uri("/health")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP")
                .jsonPath("$.service").isEqualTo("api-gateway");
    }

    @Test
    void testUnauthorizedAccessToProtectedRoute() {
        webTestClient.get()
                .uri("/api/users/profile")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void testAuthorizedAccessWithValidToken() {
        webTestClient.get()
                .uri("/api/users/profile")
                .header(HttpHeaders.AUTHORIZATION, validToken)
                .exchange()
                .expectStatus().is5xxServerError(); // Expected as backend service is not running
    }

    @Test
    void testAuthenticationWithInvalidToken() {
        when(jwtValidationService.validateToken(anyString()))
                .thenReturn(Mono.just(false));

        webTestClient.get()
                .uri("/api/users/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid_token")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void testAuthenticationWithMissingToken() {
        webTestClient.get()
                .uri("/api/users/profile")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void testFallbackController() {
        webTestClient.get()
                .uri("/fallback/auth")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.error").isEqualTo("Service Unavailable")
                .jsonPath("$.service").isEqualTo("Authentication Service");
    }

    @Test
    void testCorsHeaders() {
        webTestClient.options()
                .uri("/api/auth/login")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("Access-Control-Allow-Origin")
                .expectHeader().exists("Access-Control-Allow-Methods");
    }

    @Test
    void testRateLimitHeaders() {
        webTestClient.get()
                .uri("/api/users/profile")
                .header(HttpHeaders.AUTHORIZATION, validToken)
                .exchange()
                .expectHeader().exists("X-RateLimit-Limit");
    }
}
