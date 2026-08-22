package com.disaster.gateway.config;

import com.disaster.gateway.filter.JwtAuthenticationFilter;
import com.disaster.gateway.filter.LoggingFilter;
import com.disaster.gateway.filter.RateLimitFilter;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Gateway routing configuration for Disaster Resilience Hub
 *
 * Defines all routes to microservices with filters for:
 * - JWT authentication
 * - Rate limiting
 * - Circuit breaking
 * - Request/response logging
 *
 * @author Disaster Resilience Hub Team
 */
@Configuration
public class GatewayConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final LoggingFilter loggingFilter;
    private final RateLimitFilter rateLimitFilter;

    @Value("${services.user-session.host:user-session}")
    private String userSessionHost;

    @Value("${services.user-session.port:8081}")
    private int userSessionPort;

    @Value("${services.disaster-integrator.host:disaster-integrator}")
    private String disasterIntegratorHost;

    @Value("${services.disaster-integrator.port:8082}")
    private int disasterIntegratorPort;

    @Value("${services.disaster-visualizer.host:disaster-visualizer}")
    private String disasterVisualizerHost;

    @Value("${services.disaster-visualizer.port:8083}")
    private int disasterVisualizerPort;

    @Value("${services.llm-service.host:llm-service}")
    private String llmServiceHost;

    @Value("${services.llm-service.port:8084}")
    private int llmServicePort;

    @Value("${services.collaboration-service.host:collaboration-service}")
    private String collaborationServiceHost;

    @Value("${services.collaboration-service.port:8085}")
    private int collaborationServicePort;

    @Value("${services.ai-model.host:ai-model}")
    private String aiModelHost;

    @Value("${services.ai-model.port:8000}")
    private int aiModelPort;

    public GatewayConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                        LoggingFilter loggingFilter,
                        RateLimitFilter rateLimitFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.loggingFilter = loggingFilter;
        this.rateLimitFilter = rateLimitFilter;
    }

    /**
     * Defines all routes to microservices
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        String userSessionUri = "http://" + userSessionHost + ":" + userSessionPort;
        String disasterIntegratorUri = "http://" + disasterIntegratorHost + ":" + disasterIntegratorPort;
        String disasterVisualizerUri = "http://" + disasterVisualizerHost + ":" + disasterVisualizerPort;
        String llmServiceUri = "http://" + llmServiceHost + ":" + llmServicePort;
        String collaborationServiceUri = "http://" + collaborationServiceHost + ":" + collaborationServicePort;
        String aiModelUri = "http://" + aiModelHost + ":" + aiModelPort;

        return builder.routes()
                // Authentication Service Routes (Public)
                .route("auth-login", r -> r
                        .path("/api/auth/login", "/api/auth/register", "/api/auth/refresh")
                        .and()
                        .method(HttpMethod.POST)
                        .filters(f -> f
                                .filter(loggingFilter.apply(new LoggingFilter.Config()))
                                .filter(rateLimitFilter.apply(new RateLimitFilter.Config(10, Duration.ofSeconds(1))))
                                .circuitBreaker(config -> config
                                        .setName("auth-circuit-breaker")
                                        .setFallbackUri("forward:/fallback/auth"))
                                .rewritePath("/api/auth/(?<segment>.*)", "/${segment}"))
                        .uri(userSessionUri))

                // Authentication Service Routes (Protected)
                .route("auth-protected", r -> r
                        .path("/api/auth/**")
                        .filters(f -> f
                                .filter(loggingFilter.apply(new LoggingFilter.Config()))
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                .filter(rateLimitFilter.apply(new RateLimitFilter.Config(20, Duration.ofSeconds(1))))
                                .circuitBreaker(config -> config
                                        .setName("auth-circuit-breaker")
                                        .setFallbackUri("forward:/fallback/auth"))
                                .rewritePath("/api/auth/(?<segment>.*)", "/${segment}"))
                        .uri(userSessionUri))

                // User Service Routes
                .route("users-service", r -> r
                        .path("/api/users/**")
                        .filters(f -> f
                                .filter(loggingFilter.apply(new LoggingFilter.Config()))
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                .filter(rateLimitFilter.apply(new RateLimitFilter.Config(30, Duration.ofSeconds(1))))
                                .circuitBreaker(config -> config
                                        .setName("users-circuit-breaker")
                                        .setFallbackUri("forward:/fallback/users"))
                                .rewritePath("/api/users/(?<segment>.*)", "/users/${segment}"))
                        .uri(userSessionUri))

                // Disaster Integrator Service Routes
                .route("integrator-service", r -> r
                        .path("/api/integrator/**")
                        .filters(f -> f
                                .filter(loggingFilter.apply(new LoggingFilter.Config()))
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                .filter(rateLimitFilter.apply(new RateLimitFilter.Config(50, Duration.ofSeconds(1))))
                                .circuitBreaker(config -> config
                                        .setName("integrator-circuit-breaker")
                                        .setFallbackUri("forward:/fallback/integrator"))
                                .rewritePath("/api/integrator/(?<segment>.*)", "/${segment}"))
                        .uri(disasterIntegratorUri))

                // Disaster Visualizer Service Routes
                .route("visualizer-service", r -> r
                        .path("/api/visualizer/**")
                        .filters(f -> f
                                .filter(loggingFilter.apply(new LoggingFilter.Config()))
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                .filter(rateLimitFilter.apply(new RateLimitFilter.Config(40, Duration.ofSeconds(1))))
                                .circuitBreaker(config -> config
                                        .setName("visualizer-circuit-breaker")
                                        .setFallbackUri("forward:/fallback/visualizer"))
                                .rewritePath("/api/visualizer/(?<segment>.*)", "/${segment}"))
                        .uri(disasterVisualizerUri))

                // LLM Service Routes
                .route("llm-service", r -> r
                        .path("/api/llm/**")
                        .filters(f -> f
                                .filter(loggingFilter.apply(new LoggingFilter.Config()))
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                .filter(rateLimitFilter.apply(new RateLimitFilter.Config(15, Duration.ofSeconds(1))))
                                .circuitBreaker(config -> config
                                        .setName("llm-circuit-breaker")
                                        .setFallbackUri("forward:/fallback/llm"))
                                .rewritePath("/api/llm/(?<segment>.*)", "/${segment}"))
                        .uri(llmServiceUri))

                // Collaboration Service Routes
                .route("collaboration-service", r -> r
                        .path("/api/collaboration/**")
                        .filters(f -> f
                                .filter(loggingFilter.apply(new LoggingFilter.Config()))
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                .filter(rateLimitFilter.apply(new RateLimitFilter.Config(25, Duration.ofSeconds(1))))
                                .circuitBreaker(config -> config
                                        .setName("collaboration-circuit-breaker")
                                        .setFallbackUri("forward:/fallback/collaboration"))
                                .rewritePath("/api/collaboration/(?<segment>.*)", "/${segment}"))
                        .uri(collaborationServiceUri))

                // AI Model Service Routes
                .route("ai-model-service", r -> r
                        .path("/api/ai/**")
                        .filters(f -> f
                                .filter(loggingFilter.apply(new LoggingFilter.Config()))
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                .filter(rateLimitFilter.apply(new RateLimitFilter.Config(10, Duration.ofSeconds(1))))
                                .circuitBreaker(config -> config
                                        .setName("ai-circuit-breaker")
                                        .setFallbackUri("forward:/fallback/ai"))
                                .rewritePath("/api/ai/(?<segment>.*)", "/${segment}"))
                        .uri(aiModelUri))

                // Health Check Aggregation
                .route("health-check", r -> r
                        .path("/actuator/health/**")
                        .filters(f -> f
                                .filter(loggingFilter.apply(new LoggingFilter.Config())))
                        .uri("http://localhost:8080"))

                .build();
    }

    /**
     * Key resolver for rate limiting based on user IP address
     */
    @Bean
    @org.springframework.context.annotation.Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ipAddress = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just(ipAddress);
        };
    }

    /**
     * Key resolver for rate limiting based on user ID (from JWT)
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            return Mono.just(userId != null ? userId : "anonymous");
        };
    }

    /**
     * Redis rate limiter configuration
     */
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(100, 200, 1);
    }

    /**
     * Circuit breaker configuration with Resilience4j
     */
    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .slidingWindowSize(10)
                        .minimumNumberOfCalls(5)
                        .failureRateThreshold(50.0f)
                        .waitDurationInOpenState(Duration.ofSeconds(30))
                        .permittedNumberOfCallsInHalfOpenState(3)
                        .automaticTransitionFromOpenToHalfOpenEnabled(true)
                        .build())
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(10))
                        .build())
                .build());
    }
}
