# Disaster Resilience Hub - API Gateway

Spring Cloud Gateway-based API Gateway for the Disaster Resilience Hub microservices architecture.

## Features

- **Intelligent Routing**: Routes requests to appropriate microservices based on path patterns
- **JWT Authentication**: Validates JWT tokens on all protected routes
- **Rate Limiting**: Distributed rate limiting using Redis with configurable limits per endpoint
- **Circuit Breaker**: Resilience4j-based circuit breaker with fallback responses
- **Request/Response Logging**: Comprehensive logging with performance metrics
- **CORS Support**: Configurable CORS for cross-origin requests
- **Health Aggregation**: Aggregates health status from all microservices
- **Security Headers**: Implements security best practices with appropriate headers

## Architecture

### Routes

| Route Pattern | Target Service | Port | Rate Limit |
|--------------|----------------|------|------------|
| `/api/auth/**` | user-session | 8081 | 10-20 req/s |
| `/api/users/**` | user-session | 8081 | 30 req/s |
| `/api/integrator/**` | disaster-integrator | 8082 | 50 req/s |
| `/api/visualizer/**` | disaster-visualizer | 8083 | 40 req/s |
| `/api/llm/**` | llm-service | 8084 | 15 req/s |
| `/api/collaboration/**` | collaboration-service | 8085 | 25 req/s |
| `/api/ai/**` | ai-model | 8000 | 10 req/s |

### Components

```
api-gateway/
├── src/main/java/com/disaster/gateway/
│   ├── ApiGatewayApp.java              # Main application
│   ├── config/
│   │   ├── GatewayConfig.java          # Route definitions
│   │   └── RedisConfig.java            # Redis configuration
│   ├── filter/
│   │   ├── JwtAuthenticationFilter.java # JWT validation
│   │   ├── LoggingFilter.java          # Request/response logging
│   │   └── RateLimitFilter.java        # Rate limiting
│   ├── security/
│   │   └── SecurityConfig.java         # Security & CORS
│   ├── service/
│   │   └── JwtValidationService.java   # JWT utilities
│   └── controller/
│       ├── FallbackController.java     # Circuit breaker fallbacks
│       └── HealthAggregationController.java # Health checks
└── src/test/java/                      # Integration tests
```

## Configuration

### Environment Variables

```bash
# Server
SERVER_PORT=8080

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# JWT
JWT_SECRET=YourSuperSecretKeyForJWTTokenGenerationAndValidationMustBeLongEnough

# Service Endpoints
USER_SESSION_HOST=user-session
USER_SESSION_PORT=8081
DISASTER_INTEGRATOR_HOST=disaster-integrator
DISASTER_INTEGRATOR_PORT=8082
DISASTER_VISUALIZER_HOST=disaster-visualizer
DISASTER_VISUALIZER_PORT=8083
LLM_SERVICE_HOST=llm-service
LLM_SERVICE_PORT=8084
COLLABORATION_SERVICE_HOST=collaboration-service
COLLABORATION_SERVICE_PORT=8085
AI_MODEL_HOST=ai-model
AI_MODEL_PORT=8000
```

### Circuit Breaker Configuration

Each service has dedicated circuit breaker settings:

- **Sliding Window**: 10-15 requests
- **Failure Threshold**: 50-60%
- **Wait Duration**: 30-60 seconds
- **Timeout**: 10-30 seconds (varies by service)

## Building

### Prerequisites

- Java 17+
- Maven 3.9+
- Redis 6.0+

### Build Commands

```bash
# Build with Maven
mvn clean package

# Build without tests
mvn clean package -DskipTests

# Run tests only
mvn test
```

## Running

### Local Development

```bash
# Run with Maven
mvn spring-boot:run

# Run with Java
java -jar target/api-gateway-1.0.0.jar
```

### Docker

```bash
# Build Docker image
docker build -t disaster-hub/api-gateway:latest .

# Run container
docker run -p 8080:8080 \
  -e REDIS_HOST=redis \
  -e JWT_SECRET=your_secret_key \
  disaster-hub/api-gateway:latest
```

## Testing

### Unit Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=JwtValidationServiceTest
```

### Integration Tests

```bash
# Run integration tests
mvn verify

# Test with coverage
mvn test jacoco:report
```

### Manual Testing

```bash
# Health check
curl http://localhost:8080/actuator/health

# Aggregate health
curl http://localhost:8080/health/aggregate

# Test authentication (requires valid JWT)
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  http://localhost:8080/api/users/profile

# Test rate limiting (send multiple requests)
for i in {1..150}; do
  curl -i http://localhost:8080/actuator/health
done
```

## Monitoring

### Endpoints

- **Health**: `/actuator/health` - Gateway health status
- **Health Aggregate**: `/health/aggregate` - All services health
- **Metrics**: `/actuator/metrics` - Prometheus metrics
- **Gateway Routes**: `/actuator/gateway/routes` - Active routes

### Metrics

The gateway exposes Prometheus metrics at `/actuator/prometheus`:

- Request counts and durations
- Circuit breaker states
- Rate limit statistics
- JVM metrics

### Logs

Logs are written to:
- Console (structured logging)
- File: `logs/api-gateway.log` (rotating, 10MB max, 30 days retention)

## Security

### JWT Validation

- Validates signature using shared secret
- Checks expiration
- Extracts user information (ID, username, roles, email)
- Passes user context to downstream services via headers

### Rate Limiting

- Token bucket algorithm
- Distributed via Redis
- Per user/IP address
- Configurable limits per endpoint
- Returns `429 Too Many Requests` with `Retry-After` header

### CORS

Configured for:
- Allowed origins: localhost, production domains
- Allowed methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
- Credentials support enabled
- Preflight cache: 1 hour

### Security Headers

- Frame Options: DENY
- Content Security Policy
- Upgrade Insecure Requests

## Troubleshooting

### Common Issues

**Circuit Breaker Opens**
- Check downstream service health
- Review timeout settings
- Check failure rate threshold

**Rate Limit Exceeded**
- Verify Redis connectivity
- Check rate limit configuration
- Monitor request patterns

**JWT Validation Fails**
- Ensure JWT secret matches auth service
- Check token expiration
- Verify token format

**Service Unavailable**
- Check service discovery
- Verify service endpoints
- Review network connectivity

## Performance

### Benchmarks

- Throughput: 10,000+ req/s (without rate limiting)
- Latency: <5ms (gateway overhead)
- Memory: 512MB-1GB heap

### Optimization

- HTTP/2 enabled
- Response compression
- Connection pooling
- Reactive, non-blocking architecture

## Contributing

1. Follow Spring Boot best practices
2. Add tests for new features
3. Update documentation
4. Follow existing code style

## License

Copyright 2025 Disaster Resilience Hub Team
