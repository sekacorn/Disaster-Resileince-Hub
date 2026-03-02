package com.disaster.collaboration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

/**
 * Main Application class for Collaboration Service
 * Enables WebSocket support for real-time collaboration features
 */
@SpringBootApplication
@EnableWebSocket
@EnableCaching
@EnableScheduling
public class CollaborationApp {

    public static void main(String[] args) {
        SpringApplication.run(CollaborationApp.class, args);
        System.out.println("========================================");
        System.out.println("Collaboration Service Started Successfully");
        System.out.println("WebSocket endpoint: ws://localhost:8085/ws/collaborate/{sessionId}");
        System.out.println("REST API: http://localhost:8085/api/collaboration");
        System.out.println("========================================");
    }
}
