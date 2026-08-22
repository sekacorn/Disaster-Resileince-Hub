# LLM Service - Disaster Resilience Hub

AI-powered natural language processing microservice for disaster-related questions, emergency guidance, troubleshooting, and response planning.

## Features

- **Natural Language Query Processing**: Process disaster-related questions using external LLM providers
- **Context-Aware Responses**: Uses disaster type, location, severity, emergency status, and operational context
- **Multi-Provider Support**: Integrates with Hugging Face and xAI-compatible APIs
- **Automatic Fallback**: Switches providers or returns static guidance when the primary service is unavailable
- **Troubleshooting Assistance**: Supports equipment, communications, and response workflow questions
- **Query History**: Logs successful and failed requests for audit and follow-up
- **Emergency Mode**: Public endpoints for immediate safety tips
- **Circuit Breaker**: Resilience4j integration for fault tolerance

## Tech Stack

- **Framework**: Spring Boot 3.2
- **Language**: Java 17
- **Database**: PostgreSQL in production, H2 with the `local` profile
- **Security**: JWT Authentication
- **HTTP Client**: Spring WebFlux
- **AI Providers**: Hugging Face, xAI-compatible APIs
- **Resilience**: Resilience4j
- **Testing**: JUnit 5, Mockito

## API Endpoints

### Query Endpoints

#### `POST /api/llm/query`

Process a natural language disaster management query.

**Request:**

```json
{
  "query": "How do I prepare for an earthquake?",
  "context": {
    "disasterType": "earthquake",
    "location": "California",
    "severityLevel": 7,
    "isEmergency": false
  }
}
```

**Response:**

```json
{
  "queryId": 1,
  "response": "Start by securing heavy furniture, identifying safe interior spaces, and preparing supplies for at least 72 hours.",
  "originalQuery": "How do I prepare for an earthquake?",
  "provider": "huggingface",
  "model": "mistralai/Mistral-7B-Instruct-v0.2",
  "processingTimeMs": 2500,
  "tokensUsed": 450,
  "success": true,
  "timestamp": "2026-08-21T12:00:00",
  "recommendedActions": ["Secure heavy furniture", "Create an emergency kit"],
  "followUpQuestions": ["Do you need evacuation guidance?"]
}
```

#### `POST /api/llm/troubleshoot`

Process a disaster-response troubleshooting query.

**Request:**

```json
{
  "issue": "My emergency radio is not working",
  "context": {
    "disasterType": "general",
    "location": "field command post"
  }
}
```

### History Endpoints

#### `GET /api/llm/history/{userId}`

Get query history for a user.

**Query Parameters:**

- `page`: Page number, default `0`
- `size`: Page size, default `20`

#### `GET /api/llm/session/{sessionId}`

Get all queries in a conversation session.

#### `GET /api/llm/stats/{userId}`

Get query statistics for a user.

### Emergency Endpoints

#### `GET /api/llm/emergency-tips/{disasterType}`

Get immediate public safety tips for a disaster type.

### Health Endpoint

#### `GET /actuator/health`

Service health check.

## Local Demo Route

When the API gateway runs with the `local` profile, the frontend can use a mock chat endpoint without a database or external LLM key:

```http
POST http://localhost:8080/api/v1/llm/chat
```

The standalone LLM service still runs on port `8084`.

## Configuration

### Environment Variables

```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=disaster_llm
DB_USER=postgres
DB_PASSWORD=postgres

# Server
SERVER_PORT=8084

# LLM Configuration
LLM_PROVIDER=huggingface
LLM_API_KEY=your-api-key-here
LLM_TIMEOUT=30000
LLM_MAX_RETRIES=3

# Hugging Face
HUGGINGFACE_API_URL=https://api-inference.huggingface.co/models
HUGGINGFACE_MODEL=mistralai/Mistral-7B-Instruct-v0.2
HUGGINGFACE_MAX_TOKENS=500
HUGGINGFACE_TEMPERATURE=0.7

# xAI-compatible API
XAI_API_URL=https://api.x.ai/v1
XAI_MODEL=grok-beta
XAI_MAX_TOKENS=500
XAI_TEMPERATURE=0.7

# Fallback
LLM_FALLBACK_ENABLED=true

# JWT
JWT_SECRET=your-secret-key-here
JWT_EXPIRATION=86400000

# Logging
LOG_LEVEL=INFO
```

## Running the Service

### Local Development

```bash
mvn clean package
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Docker

```bash
docker build -t disaster-llm-service .

docker run -p 8084:8084 \
  -e DB_HOST=postgres \
  -e LLM_API_KEY=your-key \
  -e LLM_PROVIDER=huggingface \
  disaster-llm-service
```

## Testing

```bash
mvn test
mvn test -Dtest=LLMServiceTest
```

## Database Schema

### `llm_queries`

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| user_id | VARCHAR(100) | User identifier |
| query_text | TEXT | User query |
| response_text | TEXT | AI response |
| query_type | VARCHAR(50) | Query category |
| disaster_type | VARCHAR(100) | Disaster context |
| location | VARCHAR(255) | User location |
| severity_level | INTEGER | 1-10 severity |
| is_emergency | BOOLEAN | Emergency flag |
| llm_provider | VARCHAR(50) | Provider used |
| model_used | VARCHAR(100) | Model name |
| processing_time_ms | BIGINT | Processing duration |
| tokens_used | INTEGER | Token count |
| was_successful | BOOLEAN | Success flag |
| error_message | TEXT | Error details |
| context_data | TEXT | JSON context |
| created_at | TIMESTAMP | Creation time |
| session_id | VARCHAR(100) | Session identifier |

## Architecture

### Components

1. **LLMController**: REST API endpoints
2. **LLMService**: Business logic orchestration
3. **HuggingFaceClient**: Hugging Face API integration
4. **XAIClient**: xAI-compatible API integration
5. **LLMQueryRepository**: Data persistence

### Flow

```text
User Request -> Controller -> LLMService
                                |
                        Provider Selection
                                |
                        API Call + Fallback
                                |
                        Response Processing
                                |
                    Query Saved to Database
                                |
                        Return to User
```

## Monitoring

### Actuator Endpoints

- `/actuator/health` - Health status
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus metrics

### Metrics Tracked

- Query processing time
- Token usage
- Provider success and failure rates
- Circuit breaker states
- Query volume by disaster type

## Security

All endpoints except emergency and health endpoints require JWT authentication.

```http
Authorization: Bearer <jwt-token>
```

## Best Practices

1. Never commit API keys to version control.
2. Monitor token usage to avoid quota issues.
3. Provide detailed disaster context for better responses.
4. Prioritize emergency queries with clear severity and location context.
5. Track failed queries and processing times.

## Troubleshooting

**LLM API returns 401 Unauthorized**

Check that `LLM_API_KEY` is valid and has the right provider permissions.

**Slow response times**

Increase `LLM_TIMEOUT`, check network latency, or use a faster model.

**Circuit breaker opens frequently**

Check API provider status, verify rate limits, and adjust circuit breaker thresholds.

**Database connection errors**

Verify `DB_HOST`, `DB_PORT`, credentials, and database availability. For local development, use the `local` profile to run against H2.

## License

Dual-licensed under commercial and open-source licenses. See the root license files.
