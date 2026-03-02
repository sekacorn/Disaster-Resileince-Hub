# LLM Service - Disaster Resilience Hub

AI-powered natural language processing microservice for disaster-related queries with MBTI-personalized responses.

## Features

- **Natural Language Query Processing**: Process disaster-related questions using advanced LLM models
- **MBTI Personalization**: Tailored responses based on 16 personality types
- **Multi-Provider Support**: Integration with Hugging Face and xAI (Grok) APIs
- **Automatic Fallback**: Seamless provider switching when primary service is unavailable
- **Context-Aware**: Considers disaster type, location, severity, and user context
- **Troubleshooting Assistance**: Specialized troubleshooting query processing
- **Query History**: Complete logging and history tracking
- **Emergency Mode**: Public endpoints for emergency situations
- **Circuit Breaker**: Resilience4j integration for fault tolerance

## Tech Stack

- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **Database**: PostgreSQL
- **Security**: JWT Authentication
- **HTTP Client**: Spring WebFlux
- **AI Providers**: Hugging Face, xAI
- **Resilience**: Resilience4j
- **Testing**: JUnit 5, Mockito

## API Endpoints

### Query Endpoints

#### POST /api/llm/query
Process a natural language query

**Request:**
```json
{
  "query": "How do I prepare for an earthquake?",
  "context": {
    "mbtiType": "INTJ",
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
  "response": "Strategic earthquake preparation steps...",
  "originalQuery": "How do I prepare for an earthquake?",
  "mbtiType": "INTJ",
  "provider": "huggingface",
  "model": "meta-llama/Llama-2-7b-chat-hf",
  "processingTimeMs": 2500,
  "tokensUsed": 450,
  "success": true,
  "timestamp": "2025-10-20T12:00:00",
  "recommendedActions": ["Secure heavy furniture", "Create emergency kit"],
  "followUpQuestions": ["Would you like evacuation procedures?"]
}
```

#### POST /api/llm/troubleshoot
Process a troubleshooting query

**Request:**
```json
{
  "issue": "My emergency radio is not working",
  "context": {
    "mbtiType": "ISTJ",
    "disasterType": "general"
  }
}
```

### History Endpoints

#### GET /api/llm/history/{userId}
Get query history for a user

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Page size (default: 20)

**Response:**
```json
{
  "content": [
    {
      "id": 1,
      "userId": "user123",
      "queryText": "How do I prepare?",
      "responseText": "Here are the steps...",
      "mbtiType": "INTJ",
      "createdAt": "2025-10-20T12:00:00",
      "wasSuccessful": true
    }
  ],
  "totalElements": 50,
  "totalPages": 3,
  "size": 20
}
```

#### GET /api/llm/session/{sessionId}
Get all queries in a conversation session

#### GET /api/llm/stats/{userId}
Get query statistics for a user

**Response:**
```json
{
  "totalQueries": 50,
  "totalTokensUsed": 10000
}
```

### Emergency Endpoints (Public - No Auth)

#### GET /api/llm/emergency-tips/{disasterType}
Get immediate safety tips for a disaster type

**Example:** `/api/llm/emergency-tips/earthquake`

### Health Endpoint

#### GET /api/llm/health
Service health check

## MBTI Personalization

The service adapts responses based on 16 MBTI personality types:

### Analysts (Strategic & Logical)
- **INTJ**: Strategic, systematic, long-term planning
- **INTP**: Theoretical, detailed technical explanations
- **ENTJ**: Decisive, action-oriented, leadership focus
- **ENTP**: Innovative, multiple perspectives

### Diplomats (Empathetic & People-Focused)
- **INFJ**: Empathetic, meaningful, values-based
- **INFP**: Compassionate, authentic, values-aligned
- **ENFJ**: Inspiring, community coordination
- **ENFP**: Enthusiastic, creative, supportive

### Sentinels (Practical & Reliable)
- **ISTJ**: Step-by-step procedures, proven methods
- **ISFJ**: Practical support, individual needs
- **ESTJ**: Efficient, organized, results-oriented
- **ESFJ**: People-centered, cooperative

### Explorers (Adaptable & Action-Oriented)
- **ISTP**: Hands-on, technical, flexible
- **ISFP**: Compassionate, present-moment focus
- **ESTP**: Quick action, immediate impact
- **ESFP**: Enthusiastic, practical help

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
LLM_PROVIDER=huggingface  # or xai
LLM_API_KEY=your-api-key-here
LLM_TIMEOUT=30000
LLM_MAX_RETRIES=3

# Hugging Face
HUGGINGFACE_API_URL=https://api-inference.huggingface.co/models
HUGGINGFACE_MODEL=meta-llama/Llama-2-7b-chat-hf
HUGGINGFACE_MAX_TOKENS=500
HUGGINGFACE_TEMPERATURE=0.7

# xAI (Grok)
XAI_API_URL=https://api.x.ai/v1
XAI_MODEL=grok-beta
XAI_MAX_TOKENS=500
XAI_TEMPERATURE=0.7

# Fallback
LLM_FALLBACK_ENABLED=true

# JWT
JWT_SECRET=your-secret-key-here
JWT_EXPIRATION=86400000

# MBTI
MBTI_ENABLED=true
MBTI_DEFAULT=INFJ

# Logging
LOG_LEVEL=INFO
```

## LLM Provider Setup

### Hugging Face

1. Sign up at [Hugging Face](https://huggingface.co/)
2. Generate an API token
3. Set `LLM_API_KEY` to your token
4. Set `LLM_PROVIDER=huggingface`

**Recommended Models:**
- `meta-llama/Llama-2-7b-chat-hf`
- `mistralai/Mistral-7B-Instruct-v0.2`
- `google/flan-t5-xxl`

### xAI (Grok)

1. Sign up at [xAI](https://x.ai/)
2. Generate an API key
3. Set `LLM_API_KEY` to your key
4. Set `LLM_PROVIDER=xai`

**Models:**
- `grok-beta`

## Running the Service

### Local Development

```bash
# Build
mvn clean install

# Run
mvn spring-boot:run

# Run with profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Docker

```bash
# Build image
docker build -t disaster-llm-service .

# Run container
docker run -p 8084:8084 \
  -e DB_HOST=postgres \
  -e LLM_API_KEY=your-key \
  -e LLM_PROVIDER=huggingface \
  disaster-llm-service
```

### Docker Compose

```yaml
services:
  llm-service:
    build: ./backend/llm-service
    ports:
      - "8084:8084"
    environment:
      DB_HOST: postgres
      DB_NAME: disaster_llm
      LLM_API_KEY: ${LLM_API_KEY}
      LLM_PROVIDER: huggingface
      JWT_SECRET: ${JWT_SECRET}
    depends_on:
      - postgres
```

## Testing

```bash
# Run all tests
mvn test

# Run with coverage
mvn test jacoco:report

# Run specific test
mvn test -Dtest=LLMServiceTest
```

## Database Schema

### llm_queries Table

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| user_id | VARCHAR(100) | User identifier |
| query_text | TEXT | User query |
| response_text | TEXT | AI response |
| mbti_type | VARCHAR(4) | MBTI personality type |
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
4. **XAIClient**: xAI API integration
5. **MBTIPersonalizationService**: Personality-based customization
6. **LLMQueryRepository**: Data persistence

### Flow

```
User Request → Controller → LLMService → MBTI Service
                                ↓
                    Provider Selection (HF/xAI)
                                ↓
                        API Call + Fallback
                                ↓
                        Response Processing
                                ↓
                    Query Saved to Database
                                ↓
                        Return to User
```

### Resilience Features

- **Circuit Breaker**: Prevents cascading failures
- **Retry Logic**: Automatic retry with exponential backoff
- **Fallback Provider**: Switches to alternative LLM on failure
- **Static Fallback**: Returns helpful message when all providers fail
- **Timeouts**: Configurable request timeouts

## Monitoring

### Actuator Endpoints

- `/actuator/health` - Health status
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus metrics

### Metrics Tracked

- Query processing time
- Token usage
- Provider success/failure rates
- Circuit breaker states
- Query volume by disaster type
- MBTI distribution

## Security

### JWT Authentication

All endpoints (except emergency and health) require JWT authentication.

**Header:**
```
Authorization: Bearer <jwt-token>
```

### Roles

- **USER**: Standard user access
- **ADMIN**: Full access including all user histories

### Emergency Access

Public endpoints for emergency situations don't require authentication:
- `/api/llm/emergency-tips/{disasterType}`
- `/api/llm/health`

## Best Practices

1. **API Key Security**: Never commit API keys to version control
2. **Rate Limiting**: Monitor token usage to avoid quota issues
3. **Context Quality**: Provide detailed context for better responses
4. **MBTI Accuracy**: Use accurate MBTI types for optimal personalization
5. **Emergency Mode**: Prioritize emergency queries with appropriate context
6. **Monitoring**: Track failed queries and processing times

## Troubleshooting

### Common Issues

**Issue**: LLM API returns 401 Unauthorized
- **Solution**: Check `LLM_API_KEY` is valid and has proper permissions

**Issue**: Slow response times
- **Solution**: Increase `LLM_TIMEOUT`, check network latency, consider using faster models

**Issue**: Circuit breaker opens frequently
- **Solution**: Check API provider status, verify rate limits, adjust circuit breaker thresholds

**Issue**: Database connection errors
- **Solution**: Verify `DB_HOST`, `DB_PORT`, credentials, and PostgreSQL is running

## Contributing

1. Follow Java code conventions
2. Write unit tests for new features
3. Update documentation
4. Test with multiple MBTI types
5. Verify fallback scenarios

## License

Dual-licensed under commercial and open-source licenses. See LICENSE files.

## Support

For issues or questions, contact the DisasterResilienceHub team.
