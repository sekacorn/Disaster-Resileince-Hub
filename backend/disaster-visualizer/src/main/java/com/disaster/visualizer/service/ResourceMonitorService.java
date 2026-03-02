package com.disaster.visualizer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for monitoring system resources during visualization rendering
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceMonitorService {

    private final ObjectMapper objectMapper;

    @Value("${monitoring.enabled:true}")
    private boolean monitoringEnabled;

    @Value("${monitoring.thresholds.cpu-warning:70}")
    private double cpuWarningThreshold;

    @Value("${monitoring.thresholds.cpu-critical:90}")
    private double cpuCriticalThreshold;

    @Value("${monitoring.thresholds.memory-warning:75}")
    private double memoryWarningThreshold;

    @Value("${monitoring.thresholds.memory-critical:90}")
    private double memoryCriticalThreshold;

    private final Map<String, Object> currentMetrics = new ConcurrentHashMap<>();
    private final Map<String, String> alerts = new ConcurrentHashMap<>();

    /**
     * Get current resource status
     */
    public Map<String, Object> getResourceStatus() {
        if (!monitoringEnabled) {
            ObjectNode disabled = objectMapper.createObjectNode();
            disabled.put("monitoring", "disabled");
            return Map.of("status", disabled);
        }

        updateMetrics();
        return Map.of(
            "metrics", currentMetrics,
            "alerts", alerts,
            "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * Check if system can handle a new rendering task
     */
    public boolean canAcceptNewRender() {
        if (!monitoringEnabled) {
            return true;
        }

        updateMetrics();

        double cpuUsage = (double) currentMetrics.getOrDefault("cpuUsagePercent", 0.0);
        double memoryUsage = (double) currentMetrics.getOrDefault("memoryUsagePercent", 0.0);

        return cpuUsage < cpuCriticalThreshold && memoryUsage < memoryCriticalThreshold;
    }

    /**
     * Get detailed metrics for monitoring dashboard
     */
    public ObjectNode getDetailedMetrics() {
        updateMetrics();

        ObjectNode metrics = objectMapper.createObjectNode();

        // CPU metrics
        ObjectNode cpu = metrics.putObject("cpu");
        cpu.put("usage", (double) currentMetrics.getOrDefault("cpuUsagePercent", 0.0));
        cpu.put("warningThreshold", cpuWarningThreshold);
        cpu.put("criticalThreshold", cpuCriticalThreshold);
        cpu.put("status", getCpuStatus());

        // Memory metrics
        ObjectNode memory = metrics.putObject("memory");
        memory.put("used", (long) currentMetrics.getOrDefault("memoryUsedMb", 0L));
        memory.put("max", (long) currentMetrics.getOrDefault("memoryMaxMb", 0L));
        memory.put("usagePercent", (double) currentMetrics.getOrDefault("memoryUsagePercent", 0.0));
        memory.put("warningThreshold", memoryWarningThreshold);
        memory.put("criticalThreshold", memoryCriticalThreshold);
        memory.put("status", getMemoryStatus());

        // Thread metrics
        ObjectNode threads = metrics.putObject("threads");
        threads.put("active", (int) currentMetrics.getOrDefault("activeThreads", 0));
        threads.put("peak", (int) currentMetrics.getOrDefault("peakThreads", 0));

        // System info
        ObjectNode system = metrics.putObject("system");
        system.put("availableProcessors", (int) currentMetrics.getOrDefault("availableProcessors", 0));
        system.put("systemLoadAverage", (double) currentMetrics.getOrDefault("systemLoadAverage", 0.0));

        // Alerts
        metrics.set("alerts", objectMapper.valueToTree(alerts));

        return metrics;
    }

    /**
     * Scheduled task to update metrics periodically
     */
    @Scheduled(fixedDelayString = "${monitoring.interval-seconds:30}000")
    public void scheduledMetricsUpdate() {
        if (monitoringEnabled) {
            updateMetrics();
            checkThresholds();
        }
    }

    /**
     * Update all metrics
     */
    private void updateMetrics() {
        // Memory metrics
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long memoryUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long memoryMax = memoryBean.getHeapMemoryUsage().getMax();
        double memoryUsagePercent = (memoryUsed * 100.0) / memoryMax;

        currentMetrics.put("memoryUsedMb", memoryUsed / (1024 * 1024));
        currentMetrics.put("memoryMaxMb", memoryMax / (1024 * 1024));
        currentMetrics.put("memoryUsagePercent", memoryUsagePercent);

        // CPU metrics
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        double systemLoadAverage = osBean.getSystemLoadAverage();
        int availableProcessors = osBean.getAvailableProcessors();

        // Estimate CPU usage from load average
        double cpuUsagePercent = systemLoadAverage > 0
            ? Math.min((systemLoadAverage / availableProcessors) * 100, 100)
            : 0;

        currentMetrics.put("cpuUsagePercent", cpuUsagePercent);
        currentMetrics.put("systemLoadAverage", systemLoadAverage);
        currentMetrics.put("availableProcessors", availableProcessors);

        // Thread metrics
        currentMetrics.put("activeThreads", Thread.activeCount());
        currentMetrics.put("peakThreads", ManagementFactory.getThreadMXBean().getPeakThreadCount());

        // GPU would require native libraries (CUDA, OpenCL)
        // Placeholder for GPU metrics
        currentMetrics.put("gpuUsagePercent", 0.0);
        currentMetrics.put("gpuAvailable", false);
    }

    /**
     * Check thresholds and generate alerts
     */
    private void checkThresholds() {
        alerts.clear();

        double cpuUsage = (double) currentMetrics.getOrDefault("cpuUsagePercent", 0.0);
        double memoryUsage = (double) currentMetrics.getOrDefault("memoryUsagePercent", 0.0);

        // CPU alerts
        if (cpuUsage >= cpuCriticalThreshold) {
            alerts.put("cpu", "CRITICAL: CPU usage at " + String.format("%.1f%%", cpuUsage));
            log.warn("CRITICAL: CPU usage at {}%", cpuUsage);
        } else if (cpuUsage >= cpuWarningThreshold) {
            alerts.put("cpu", "WARNING: CPU usage at " + String.format("%.1f%%", cpuUsage));
            log.warn("WARNING: CPU usage at {}%", cpuUsage);
        }

        // Memory alerts
        if (memoryUsage >= memoryCriticalThreshold) {
            alerts.put("memory", "CRITICAL: Memory usage at " + String.format("%.1f%%", memoryUsage));
            log.warn("CRITICAL: Memory usage at {}%", memoryUsage);
        } else if (memoryUsage >= memoryWarningThreshold) {
            alerts.put("memory", "WARNING: Memory usage at " + String.format("%.1f%%", memoryUsage));
            log.warn("WARNING: Memory usage at {}%", memoryUsage);
        }
    }

    private String getCpuStatus() {
        double cpuUsage = (double) currentMetrics.getOrDefault("cpuUsagePercent", 0.0);
        if (cpuUsage >= cpuCriticalThreshold) return "CRITICAL";
        if (cpuUsage >= cpuWarningThreshold) return "WARNING";
        return "NORMAL";
    }

    private String getMemoryStatus() {
        double memoryUsage = (double) currentMetrics.getOrDefault("memoryUsagePercent", 0.0);
        if (memoryUsage >= memoryCriticalThreshold) return "CRITICAL";
        if (memoryUsage >= memoryWarningThreshold) return "WARNING";
        return "NORMAL";
    }

    /**
     * Get rendering capacity estimate
     */
    public int getEstimatedRenderingCapacity() {
        if (!canAcceptNewRender()) {
            return 0;
        }

        double cpuUsage = (double) currentMetrics.getOrDefault("cpuUsagePercent", 0.0);
        double memoryUsage = (double) currentMetrics.getOrDefault("memoryUsagePercent", 0.0);

        // Simple capacity estimation
        double cpuCapacity = (100 - cpuUsage) / 10; // Each render uses ~10% CPU
        double memoryCapacity = (100 - memoryUsage) / 5; // Each render uses ~5% memory

        return (int) Math.min(cpuCapacity, memoryCapacity);
    }
}
