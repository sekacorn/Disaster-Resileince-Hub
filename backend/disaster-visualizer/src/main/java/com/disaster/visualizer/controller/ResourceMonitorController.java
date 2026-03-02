package com.disaster.visualizer.controller;

import com.disaster.visualizer.service.ResourceMonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for resource monitoring
 */
@RestController
@RequestMapping("/resources")
@RequiredArgsConstructor
@Slf4j
public class ResourceMonitorController {

    private final ResourceMonitorService resourceMonitorService;

    /**
     * Get current resource status
     * GET /api/visualizer/resources/status
     */
    @GetMapping("/status")
    public ResponseEntity<?> getResourceStatus() {
        try {
            return ResponseEntity.ok(resourceMonitorService.getResourceStatus());
        } catch (Exception e) {
            log.error("Error retrieving resource status", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get detailed metrics for monitoring dashboard
     * GET /api/visualizer/resources/metrics
     */
    @GetMapping("/metrics")
    public ResponseEntity<?> getDetailedMetrics() {
        try {
            return ResponseEntity.ok(resourceMonitorService.getDetailedMetrics());
        } catch (Exception e) {
            log.error("Error retrieving detailed metrics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Check if system can accept new rendering tasks
     * GET /api/visualizer/resources/availability
     */
    @GetMapping("/availability")
    public ResponseEntity<?> checkAvailability() {
        try {
            boolean available = resourceMonitorService.canAcceptNewRender();
            int capacity = resourceMonitorService.getEstimatedRenderingCapacity();

            return ResponseEntity.ok(new Object() {
                public final boolean available = resourceMonitorService.canAcceptNewRender();
                public final int estimatedCapacity = capacity;
                public final String status = available ? "AVAILABLE" : "BUSY";
            });
        } catch (Exception e) {
            log.error("Error checking availability", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get health status for service monitoring
     * GET /api/visualizer/resources/health
     */
    @GetMapping("/health")
    public ResponseEntity<?> getHealth() {
        try {
            boolean healthy = resourceMonitorService.canAcceptNewRender();
            return ResponseEntity.ok(new Object() {
                public final String status = healthy ? "UP" : "DEGRADED";
                public final boolean accepting_requests = healthy;
            });
        } catch (Exception e) {
            log.error("Error retrieving health status", e);
            return ResponseEntity.status(503).body(new Object() {
                public final String status = "DOWN";
                public final String error = e.getMessage();
            });
        }
    }
}
