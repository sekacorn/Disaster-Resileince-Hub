package com.disaster.gateway.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Health Aggregation Controller
 *
 * Aggregates health status from all microservices.
 * Provides a comprehensive health check endpoint.
 *
 * @author Disaster Resilience Hub Team
 */
@RestController
@RequestMapping("/health")
public class HealthAggregationController {

    private final WebClient webClient;

    @Value("${services.user-session.host:user-session}")
    private String userSessionHost;

    @Value("${services.user-session.port:8081}")
    private int userSessionPort;

    @Value("${services.disaster-integrator.host:disaster-integrator}")
    private String integratorHost;

    @Value("${services.disaster-integrator.port:8082}")
    private int integratorPort;

    @Value("${services.disaster-visualizer.host:disaster-visualizer}")
    private String visualizerHost;

    @Value("${services.disaster-visualizer.port:8083}")
    private int visualizerPort;

    @Value("${services.llm-service.host:llm-service}")
    private String llmHost;

    @Value("${services.llm-service.port:8084}")
    private int llmPort;

    @Value("${services.collaboration-service.host:collaboration-service}")
    private String collaborationHost;

    @Value("${services.collaboration-service.port:8085}")
    private int collaborationPort;

    @Value("${services.ai-model.host:ai-model}")
    private String aiModelHost;

    @Value("${services.ai-model.port:8000}")
    private int aiModelPort;

    public HealthAggregationController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * Aggregate health status from all services
     */
    @GetMapping("/aggregate")
    public Mono<ResponseEntity<Map<String, Object>>> aggregateHealth() {
        Map<String, Object> healthStatus = new HashMap<>();
        healthStatus.put("gateway", "UP");

        return Mono.zip(
                checkServiceHealth("user-session", userSessionHost, userSessionPort),
                checkServiceHealth("disaster-integrator", integratorHost, integratorPort),
                checkServiceHealth("disaster-visualizer", visualizerHost, visualizerPort),
                checkServiceHealth("llm-service", llmHost, llmPort),
                checkServiceHealth("collaboration-service", collaborationHost, collaborationPort),
                checkServiceHealth("ai-model", aiModelHost, aiModelPort)
        ).map(tuple -> {
            healthStatus.put("services", Map.of(
                    "user-session", tuple.getT1(),
                    "disaster-integrator", tuple.getT2(),
                    "disaster-visualizer", tuple.getT3(),
                    "llm-service", tuple.getT4(),
                    "collaboration-service", tuple.getT5(),
                    "ai-model", tuple.getT6()
            ));

            // Determine overall status
            boolean allUp = tuple.getT1().equals("UP") &&
                    tuple.getT2().equals("UP") &&
                    tuple.getT3().equals("UP") &&
                    tuple.getT4().equals("UP") &&
                    tuple.getT5().equals("UP") &&
                    tuple.getT6().equals("UP");

            healthStatus.put("status", allUp ? "UP" : "DEGRADED");

            return ResponseEntity.ok(healthStatus);
        }).defaultIfEmpty(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("status", "DOWN", "gateway", "UP")));
    }

    /**
     * Check health of a specific service
     */
    private Mono<String> checkServiceHealth(String serviceName, String host, int port) {
        String url = String.format("http://%s:%d/actuator/health", host, port);

        return webClient.get()
                .uri(url)
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofSeconds(5))
                .map(response -> response.getStatusCode().is2xxSuccessful() ? "UP" : "DOWN")
                .onErrorReturn("DOWN");
    }

    /**
     * Simple gateway health check
     */
    @GetMapping
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "api-gateway"
        ));
    }
}
