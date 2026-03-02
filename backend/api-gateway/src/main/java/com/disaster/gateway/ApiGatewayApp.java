package com.disaster.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Main application class for Disaster Resilience Hub API Gateway
 *
 * This gateway serves as the single entry point for all client requests,
 * routing them to appropriate microservices with security, rate limiting,
 * and monitoring capabilities.
 *
 * @author Disaster Resilience Hub Team
 * @version 1.0
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApp {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApp.class, args);
    }
}
