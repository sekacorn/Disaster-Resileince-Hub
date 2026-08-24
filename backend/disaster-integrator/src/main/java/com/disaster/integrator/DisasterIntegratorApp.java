package com.disaster.integrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Disaster Integrator Microservice Application
 *
 * This service integrates real-time environmental, community, and individual data
 * from multiple sources including NOAA weather data, USGS seismic data,
 * OpenStreetMap community data, and FHIR-compliant health records.
 *
 * Features:
 * - Multi-source data ingestion (CSV, JSON, GeoJSON, FHIR)
 * - Data validation and parsing
 * - PostgreSQL data storage
 * - RESTful APIs for data upload and retrieval
 * - JWT-based security
 *
 * @author Disaster Resilience Hub Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
// Required for RetentionPolicyService: without it the @Scheduled sweep never
// runs and the stated retention period is not actually enforced.
@EnableScheduling
public class DisasterIntegratorApp {

    public static void main(String[] args) {
        SpringApplication.run(DisasterIntegratorApp.class, args);
        System.out.println("=".repeat(70));
        System.out.println("Disaster Integrator Microservice Started Successfully");
        System.out.println("=".repeat(70));
    }
}
