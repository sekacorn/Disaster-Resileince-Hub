package com.disaster.integrator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for the main application
 */
@SpringBootTest
@ActiveProfiles("test")
class DisasterIntegratorAppTests {

    @Test
    void contextLoads() {
        // Test that the application context loads successfully
    }
}
