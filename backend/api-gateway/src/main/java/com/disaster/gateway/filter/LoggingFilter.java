package com.disaster.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

/**
 * Logging Filter for API Gateway
 *
 * Logs all incoming requests and outgoing responses with:
 * - Request method, URI, headers
 * - Response status code
 * - Request/response duration
 * - User information (if authenticated)
 *
 * @author Disaster Resilience Hub Team
 */
@Component
public class LoggingFilter extends AbstractGatewayFilterFactory<LoggingFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    public LoggingFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            Instant startTime = Instant.now();

            // Log request information
            logRequest(request);

            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                ServerHttpResponse response = exchange.getResponse();
                Instant endTime = Instant.now();
                Duration duration = Duration.between(startTime, endTime);

                // Log response information
                logResponse(request, response, duration);
            }));
        };
    }

    /**
     * Log incoming request details
     */
    private void logRequest(ServerHttpRequest request) {
        String method = request.getMethod().name();
        String path = request.getPath().value();
        String queryParams = request.getURI().getQuery() != null ? "?" + request.getURI().getQuery() : "";
        String remoteAddress = request.getRemoteAddress() != null
                ? request.getRemoteAddress().getAddress().getHostAddress()
                : "unknown";

        // Extract user information if available
        String userId = request.getHeaders().getFirst("X-User-Id");
        String username = request.getHeaders().getFirst("X-Username");

        StringBuilder logMessage = new StringBuilder();
        logMessage.append("Incoming Request: ")
                .append(method).append(" ")
                .append(path).append(queryParams)
                .append(" | IP: ").append(remoteAddress);

        if (username != null) {
            logMessage.append(" | User: ").append(username);
            if (userId != null) {
                logMessage.append(" (ID: ").append(userId).append(")");
            }
        }

        // Log important headers (excluding sensitive information)
        HttpHeaders headers = request.getHeaders();
        if (headers.getContentType() != null) {
            logMessage.append(" | Content-Type: ").append(headers.getContentType());
        }
        if (headers.getContentLength() > 0) {
            logMessage.append(" | Content-Length: ").append(headers.getContentLength());
        }

        logger.info(logMessage.toString());
    }

    /**
     * Log outgoing response details
     */
    private void logResponse(ServerHttpRequest request, ServerHttpResponse response, Duration duration) {
        String method = request.getMethod().name();
        String path = request.getPath().value();
        int statusCode = response.getStatusCode() != null ? response.getStatusCode().value() : 0;
        long durationMs = duration.toMillis();

        StringBuilder logMessage = new StringBuilder();
        logMessage.append("Outgoing Response: ")
                .append(method).append(" ")
                .append(path)
                .append(" | Status: ").append(statusCode)
                .append(" | Duration: ").append(durationMs).append("ms");

        // Log response headers
        HttpHeaders headers = response.getHeaders();
        if (headers.getContentType() != null) {
            logMessage.append(" | Content-Type: ").append(headers.getContentType());
        }
        if (headers.getContentLength() > 0) {
            logMessage.append(" | Content-Length: ").append(headers.getContentLength());
        }

        // Log level based on status code
        if (statusCode >= 500) {
            logger.error(logMessage.toString());
        } else if (statusCode >= 400) {
            logger.warn(logMessage.toString());
        } else if (durationMs > 5000) {
            // Warn if request took more than 5 seconds
            logger.warn(logMessage.toString() + " | SLOW REQUEST");
        } else {
            logger.info(logMessage.toString());
        }
    }

    /**
     * Configuration class for Logging Filter
     */
    public static class Config {
        private boolean logRequestHeaders = true;
        private boolean logResponseHeaders = true;
        private boolean logRequestBody = false; // Disabled by default for performance
        private boolean logResponseBody = false; // Disabled by default for performance

        public boolean isLogRequestHeaders() {
            return logRequestHeaders;
        }

        public void setLogRequestHeaders(boolean logRequestHeaders) {
            this.logRequestHeaders = logRequestHeaders;
        }

        public boolean isLogResponseHeaders() {
            return logResponseHeaders;
        }

        public void setLogResponseHeaders(boolean logResponseHeaders) {
            this.logResponseHeaders = logResponseHeaders;
        }

        public boolean isLogRequestBody() {
            return logRequestBody;
        }

        public void setLogRequestBody(boolean logRequestBody) {
            this.logRequestBody = logRequestBody;
        }

        public boolean isLogResponseBody() {
            return logResponseBody;
        }

        public void setLogResponseBody(boolean logResponseBody) {
            this.logResponseBody = logResponseBody;
        }
    }
}
