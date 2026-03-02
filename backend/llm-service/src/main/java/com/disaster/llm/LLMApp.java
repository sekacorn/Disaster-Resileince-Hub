package com.disaster.llm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main application class for the LLM Service.
 *
 * This microservice provides AI-powered natural language processing capabilities
 * for the Disaster Resilience Hub, including:
 * - Natural language query processing for disaster-related questions
 * - MBTI-personalized responses
 * - Integration with Hugging Face and xAI APIs
 * - Context-aware recommendations
 * - Troubleshooting assistance
 * - Query history and logging
 *
 * @author DisasterResilienceHub Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableAsync
public class LLMApp {

    public static void main(String[] args) {
        SpringApplication.run(LLMApp.class, args);
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                                                          ║");
        System.out.println("║      LLM Service - Disaster Resilience Hub              ║");
        System.out.println("║      AI-Powered Natural Language Processing              ║");
        System.out.println("║                                                          ║");
        System.out.println("║      Status: Running                                     ║");
        System.out.println("║      Port: 8084                                          ║");
        System.out.println("║                                                          ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
    }
}
