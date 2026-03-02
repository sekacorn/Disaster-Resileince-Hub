package com.disaster.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

/**
 * Rate Limiting Filter for API Gateway
 *
 * Implements token bucket algorithm using Redis for distributed rate limiting.
 * Limits requests per user/IP address based on configured thresholds.
 *
 * @author Disaster Resilience Hub Team
 */
@Component
public class RateLimitFilter extends AbstractGatewayFilterFactory<RateLimitFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public RateLimitFilter(ReactiveRedisTemplate<String, String> redisTemplate) {
        super(Config.class);
        this.redisTemplate = redisTemplate;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // Determine the key for rate limiting (user ID or IP address)
            String rateLimitKey = getRateLimitKey(request);

            return checkRateLimit(rateLimitKey, config)
                    .flatMap(allowed -> {
                        if (!allowed) {
                            logger.warn("Rate limit exceeded for key: {}", rateLimitKey);
                            return onRateLimitExceeded(exchange, config);
                        }

                        // Add rate limit headers to response
                        return getRemainingRequests(rateLimitKey, config)
                                .flatMap(remaining -> {
                                    exchange.getResponse().getHeaders()
                                            .add("X-RateLimit-Limit", String.valueOf(config.getMaxRequests()));
                                    exchange.getResponse().getHeaders()
                                            .add("X-RateLimit-Remaining", String.valueOf(remaining));
                                    exchange.getResponse().getHeaders()
                                            .add("X-RateLimit-Reset", String.valueOf(
                                                    Instant.now().plus(config.getWindow()).getEpochSecond()));

                                    return chain.filter(exchange);
                                });
                    });
        };
    }

    /**
     * Get the rate limit key (user ID or IP address)
     */
    private String getRateLimitKey(ServerHttpRequest request) {
        // Try to get user ID from headers (set by JWT filter)
        String userId = request.getHeaders().getFirst("X-User-Id");
        if (userId != null) {
            return RATE_LIMIT_KEY_PREFIX + "user:" + userId;
        }

        // Fall back to IP address
        String ipAddress = request.getRemoteAddress() != null
                ? request.getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
        return RATE_LIMIT_KEY_PREFIX + "ip:" + ipAddress;
    }

    /**
     * Check if the request is within the rate limit
     */
    private Mono<Boolean> checkRateLimit(String key, Config config) {
        String requestKey = key + ":" + getCurrentWindow(config.getWindow());

        return redisTemplate.opsForValue()
                .increment(requestKey)
                .flatMap(count -> {
                    if (count == 1) {
                        // First request in this window, set expiration
                        return redisTemplate.expire(requestKey, config.getWindow())
                                .thenReturn(true);
                    }

                    // Check if count exceeds limit
                    return Mono.just(count <= config.getMaxRequests());
                })
                .onErrorReturn(true); // Allow request if Redis is down
    }

    /**
     * Get remaining requests for this window
     */
    private Mono<Long> getRemainingRequests(String key, Config config) {
        String requestKey = key + ":" + getCurrentWindow(config.getWindow());

        return redisTemplate.opsForValue()
                .get(requestKey)
                .map(Long::parseLong)
                .map(count -> Math.max(0, config.getMaxRequests() - count))
                .defaultIfEmpty(config.getMaxRequests())
                .onErrorReturn(config.getMaxRequests());
    }

    /**
     * Get current time window based on the configured duration
     */
    private long getCurrentWindow(Duration window) {
        long windowSeconds = window.getSeconds();
        return Instant.now().getEpochSecond() / windowSeconds;
    }

    /**
     * Handle rate limit exceeded response
     */
    private Mono<Void> onRateLimitExceeded(org.springframework.web.server.ServerWebExchange exchange, Config config) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
        response.getHeaders().add("X-RateLimit-Limit", String.valueOf(config.getMaxRequests()));
        response.getHeaders().add("X-RateLimit-Remaining", "0");
        response.getHeaders().add("X-RateLimit-Reset", String.valueOf(
                Instant.now().plus(config.getWindow()).getEpochSecond()));

        long retryAfterSeconds = config.getWindow().getSeconds();
        response.getHeaders().add("Retry-After", String.valueOf(retryAfterSeconds));

        String errorResponse = String.format(
                "{\"error\":\"Rate limit exceeded\",\"status\":429,\"retryAfter\":%d,\"limit\":%d}",
                retryAfterSeconds, config.getMaxRequests());

        return response.writeWith(Mono.just(response.bufferFactory()
                .wrap(errorResponse.getBytes())));
    }

    /**
     * Configuration class for Rate Limit Filter
     */
    public static class Config {
        private long maxRequests = 100;
        private Duration window = Duration.ofMinutes(1);

        public Config() {
        }

        public Config(long maxRequests, Duration window) {
            this.maxRequests = maxRequests;
            this.window = window;
        }

        public long getMaxRequests() {
            return maxRequests;
        }

        public void setMaxRequests(long maxRequests) {
            this.maxRequests = maxRequests;
        }

        public Duration getWindow() {
            return window;
        }

        public void setWindow(Duration window) {
            this.window = window;
        }
    }
}
