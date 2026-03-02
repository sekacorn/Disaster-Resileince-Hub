package com.disaster.visualizer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

/**
 * Main application class for Disaster Visualizer Microservice
 *
 * This service provides:
 * - 3D visualization data generation for disaster maps
 * - Integration with disaster data from disaster-integrator
 * - Export functionality (PNG, SVG, STL metadata)
 * - MBTI-tailored visualization preferences
 * - Resource monitoring for rendering
 */
@SpringBootApplication
@EnableScheduling
public class DisasterVisualizerApp {

    public static void main(String[] args) {
        SpringApplication.run(DisasterVisualizerApp.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
