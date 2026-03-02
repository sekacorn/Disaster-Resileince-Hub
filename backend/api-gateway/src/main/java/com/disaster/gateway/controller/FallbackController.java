package com.disaster.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Fallback Controller for Circuit Breaker
 *
 * Provides fallback responses when services are unavailable.
 *
 * @author Disaster Resilience Hub Team
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/auth")
    public ResponseEntity<Map<String, Object>> authFallback() {
        return createFallbackResponse("Authentication Service",
                "The authentication service is temporarily unavailable. Please try again later.");
    }

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> usersFallback() {
        return createFallbackResponse("User Service",
                "The user service is temporarily unavailable. Please try again later.");
    }

    @GetMapping("/integrator")
    public ResponseEntity<Map<String, Object>> integratorFallback() {
        return createFallbackResponse("Disaster Integrator Service",
                "The disaster integrator service is temporarily unavailable. Please try again later.");
    }

    @GetMapping("/visualizer")
    public ResponseEntity<Map<String, Object>> visualizerFallback() {
        return createFallbackResponse("Disaster Visualizer Service",
                "The disaster visualizer service is temporarily unavailable. Please try again later.");
    }

    @GetMapping("/llm")
    public ResponseEntity<Map<String, Object>> llmFallback() {
        return createFallbackResponse("LLM Service",
                "The LLM service is temporarily unavailable. Please try again later.");
    }

    @GetMapping("/collaboration")
    public ResponseEntity<Map<String, Object>> collaborationFallback() {
        return createFallbackResponse("Collaboration Service",
                "The collaboration service is temporarily unavailable. Please try again later.");
    }

    @GetMapping("/ai")
    public ResponseEntity<Map<String, Object>> aiFallback() {
        return createFallbackResponse("AI Model Service",
                "The AI model service is temporarily unavailable. Please try again later.");
    }

    /**
     * Create a standardized fallback response
     */
    private ResponseEntity<Map<String, Object>> createFallbackResponse(String serviceName, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Service Unavailable");
        response.put("service", serviceName);
        response.put("message", message);
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("suggestion", "Please check back in a few moments or contact support if the issue persists.");

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response);
    }
}
