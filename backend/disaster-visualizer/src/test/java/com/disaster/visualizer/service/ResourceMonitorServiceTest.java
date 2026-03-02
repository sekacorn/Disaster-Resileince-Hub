package com.disaster.visualizer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ResourceMonitorService
 */
@ExtendWith(MockitoExtension.class)
class ResourceMonitorServiceTest {

    @InjectMocks
    private ResourceMonitorService resourceMonitorService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(resourceMonitorService, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(resourceMonitorService, "monitoringEnabled", true);
        ReflectionTestUtils.setField(resourceMonitorService, "cpuWarningThreshold", 70.0);
        ReflectionTestUtils.setField(resourceMonitorService, "cpuCriticalThreshold", 90.0);
        ReflectionTestUtils.setField(resourceMonitorService, "memoryWarningThreshold", 75.0);
        ReflectionTestUtils.setField(resourceMonitorService, "memoryCriticalThreshold", 90.0);
    }

    @Test
    void testGetResourceStatus_MonitoringEnabled() {
        // Act
        Map<String, Object> status = resourceMonitorService.getResourceStatus();

        // Assert
        assertNotNull(status);
        assertTrue(status.containsKey("metrics"));
        assertTrue(status.containsKey("alerts"));
        assertTrue(status.containsKey("timestamp"));
    }

    @Test
    void testGetResourceStatus_MonitoringDisabled() {
        // Arrange
        ReflectionTestUtils.setField(resourceMonitorService, "monitoringEnabled", false);

        // Act
        Map<String, Object> status = resourceMonitorService.getResourceStatus();

        // Assert
        assertNotNull(status);
        assertTrue(status.containsKey("status"));
    }

    @Test
    void testCanAcceptNewRender() {
        // Act
        boolean canAccept = resourceMonitorService.canAcceptNewRender();

        // Assert
        // Should return true or false based on current system resources
        // We just verify it doesn't throw an exception
        assertNotNull(canAccept);
    }

    @Test
    void testGetDetailedMetrics() {
        // Act
        ObjectNode metrics = resourceMonitorService.getDetailedMetrics();

        // Assert
        assertNotNull(metrics);
        assertTrue(metrics.has("cpu"));
        assertTrue(metrics.has("memory"));
        assertTrue(metrics.has("threads"));
        assertTrue(metrics.has("system"));
        assertTrue(metrics.has("alerts"));

        // Verify CPU metrics
        ObjectNode cpu = (ObjectNode) metrics.get("cpu");
        assertTrue(cpu.has("usage"));
        assertTrue(cpu.has("warningThreshold"));
        assertTrue(cpu.has("criticalThreshold"));
        assertTrue(cpu.has("status"));

        // Verify memory metrics
        ObjectNode memory = (ObjectNode) metrics.get("memory");
        assertTrue(memory.has("used"));
        assertTrue(memory.has("max"));
        assertTrue(memory.has("usagePercent"));
        assertTrue(memory.has("status"));
    }

    @Test
    void testGetEstimatedRenderingCapacity() {
        // Act
        int capacity = resourceMonitorService.getEstimatedRenderingCapacity();

        // Assert
        assertTrue(capacity >= 0);
    }

    @Test
    void testScheduledMetricsUpdate() {
        // This test verifies the method doesn't throw exceptions
        // Act & Assert
        assertDoesNotThrow(() ->
            resourceMonitorService.scheduledMetricsUpdate()
        );
    }

    @Test
    void testGetDetailedMetrics_CpuStatus() {
        // Act
        ObjectNode metrics = resourceMonitorService.getDetailedMetrics();

        // Assert
        ObjectNode cpu = (ObjectNode) metrics.get("cpu");
        String status = cpu.get("status").asText();
        assertTrue(status.equals("NORMAL") || status.equals("WARNING") || status.equals("CRITICAL"));
    }

    @Test
    void testGetDetailedMetrics_MemoryStatus() {
        // Act
        ObjectNode metrics = resourceMonitorService.getDetailedMetrics();

        // Assert
        ObjectNode memory = (ObjectNode) metrics.get("memory");
        String status = memory.get("status").asText();
        assertTrue(status.equals("NORMAL") || status.equals("WARNING") || status.equals("CRITICAL"));
    }

    @Test
    void testGetDetailedMetrics_ThreadMetrics() {
        // Act
        ObjectNode metrics = resourceMonitorService.getDetailedMetrics();

        // Assert
        ObjectNode threads = (ObjectNode) metrics.get("threads");
        assertTrue(threads.get("active").asInt() > 0);
        assertTrue(threads.get("peak").asInt() > 0);
    }

    @Test
    void testGetDetailedMetrics_SystemMetrics() {
        // Act
        ObjectNode metrics = resourceMonitorService.getDetailedMetrics();

        // Assert
        ObjectNode system = (ObjectNode) metrics.get("system");
        assertTrue(system.get("availableProcessors").asInt() > 0);
    }
}
