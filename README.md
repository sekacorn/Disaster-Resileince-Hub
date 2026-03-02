# DisasterResilienceHub

[![License: Dual](https://img.shields.io/badge/License-Dual-blue.svg)](./LICENSE)
[![Java](https://img.shields.io/badge/Java-17-red.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue.svg)](https://react.dev/)
[![Python](https://img.shields.io/badge/Python-3.10-yellow.svg)](https://www.python.org/)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://www.docker.com/)

**A production-ready, full-stack disaster resilience platform integrating real-time environmental data, AI predictions, 3D visualizations, and collaborative tools to enhance disaster preparedness and response.**

---

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Architecture](#architecture)
- [Licensing](#licensing)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [API Documentation](#api-documentation)
- [User Roles & Authentication](#user-roles--authentication)
- [Enterprise Features](#enterprise-features)
- [Development](#development)
- [Testing](#testing)
- [Deployment](#deployment)
- [Contributing](#contributing)
- [Support](#support)

---

## Overview

DisasterResilienceHub addresses the critical need for integrated disaster preparedness and response tools. With an estimated 1.3 million disaster-related deaths since 1990 (UNDRR), this platform empowers individuals, emergency responders, and policymakers with:

- **Real-time data integration** from NOAA, USGS, and OpenStreetMap
- **AI-driven risk predictions** and personalized evacuation planning
- **3D disaster impact visualizations** using Three.js
- **FHIR-compliant health record integration** for personalized care
- **Real-time collaboration tools** for coordinated response
- **Natural language queries** via integrated LLM
- **MBTI-tailored user interfaces** for all 16 personality types
- **Multi-language support** for global accessibility

---

## Key Features

### Authentication & Authorization
- Standard username/password authentication with BCrypt hashing
- JWT-based stateless authentication
- **Role-based access control**: User, Moderator, Admin
- Session management with Redis caching
- Account security (lockout after failed attempts)

### Enterprise Features (Commercial License Required)
- **Single Sign-On (SSO)**: SAML 2.0, OAuth2, OIDC support
- **Multi-Factor Authentication (MFA)**: TOTP, SMS, Email, Authenticator apps
- QR code generation for easy MFA setup
- Auto-provisioning from SSO providers
- Priority support and SLA agreements

### Data Integration
- **Environmental Data**: Real-time weather (NOAA), seismic activity (USGS)
- **Community Data**: Facilities, shelters, hospitals (OpenStreetMap GeoJSON)
- **Health Records**: FHIR R4-compliant patient data
- CSV, JSON, GeoJSON format support
- Comprehensive data validation

### AI Predictions
- PyTorch-based disaster risk assessment
- Personalized evacuation route planning
- Multi-factor risk analysis (environmental, community, individual)
- Confidence scoring and recommendations
- Support for: floods, earthquakes, hurricanes, tsunamis, wildfires, tornadoes

### 3D Visualizations
- Interactive 3D disaster impact maps
- Evacuation route visualization
- Three.js rendering with zoom/pan controls
- Export to PNG, SVG, STL formats
- MBTI-tailored visual styles

### LLM Integration
- Natural language disaster queries
- MBTI-personalized responses
- Troubleshooting assistance
- Strategic advice for leaders (ENTJ), empathetic for advocates (INFJ)

### Real-Time Collaboration
- WebSocket-based collaborative sessions
- Shared evacuation planning
- Interactive annotations
- Role-based collaboration tools

---

## Architecture

DisasterResilienceHub uses a microservices architecture with the following components:

```
┌─────────────────┐
│   NGINX Proxy   │  Port 80/443
└────────┬────────┘
         │
┌────────┴────────┐
│  API Gateway    │  Port 8080 (Routes & Auth)
└────────┬────────┘
         │
    ┌────┴─────────────────────────────┐
    │                                   │
┌───┴────────────┐             ┌───────┴──────────┐
│  Microservices │             │   AI/ML Service  │
└───┬────────────┘             └──────────────────┘
    │
┌───┴─────────────────────────────────────────┐
│ • User Session (Auth, SSO, MFA) - Port 8081│
│ • Disaster Integrator (Data) - Port 8082   │
│ • Disaster Visualizer (3D) - Port 8083     │
│ • LLM Service (Queries) - Port 8084        │
│ • Collaboration (WebSocket) - Port 8085    │
│ • AI Model (Python/FastAPI) - Port 8000    │
└─────────────────────────────────────────────┘
         │
    ┌────┴────────┐
    │  Databases  │
    │ PostgreSQL  │  Port 5432
    │   Redis     │  Port 6379
    └─────────────┘
```

**Frontend**: React 18 + Three.js + Tailwind CSS (Port 3000)

---

## Licensing

DisasterResilienceHub uses a **dual licensing model**:

### Open Source License (Non-Profit)
- **FREE** for non-profit organizations, educational institutions, and humanitarian efforts
- Apache 2.0 License
- All core features included
- Community support

[View Open Source License](./LICENSE-OPEN-SOURCE.md)

### Commercial License (For-Profit)
- **4% of gross revenue** attributable to the software
- Includes enterprise features: SSO, MFA, priority support
- Full commercial rights
- Dedicated technical support
- Custom SLA agreements

[View Commercial License](./LICENSE-COMMERCIAL.md)

**Contact for licensing**: licensing@disasterresiliencehub.org

---

## Prerequisites

- **Docker** & **Docker Compose** (recommended)
- **Java 17** (for local development)
- **Node.js 18+** (for frontend)
- **Python 3.10+** (for AI service)
- **PostgreSQL 16** (if not using Docker)
- **Redis 7** (if not using Docker)
- **Git**

---

## Quick Start

### Using Docker Compose (Recommended)

1. **Clone the repository**
   ```bash
   git clone https://github.com/sekacorn/Disaster-Resileince-Hub.git
   cd DisasterResilienceHub
   ```

2. **Create environment file**
   ```bash
   cp .env.example .env
   ```

3. **Edit `.env` with your configuration**
   ```env
   POSTGRES_PASSWORD=your_secure_password
   REDIS_PASSWORD=your_redis_password
   JWT_SECRET=your_jwt_secret_minimum_256_bits
   LLM_API_KEY=your_xai_or_huggingface_key
   SPRING_PROFILE=prod
   ```

4. **Start all services**
   ```bash
   docker-compose up -d
   ```

5. **Initialize database** (first run only)
   ```bash
   docker-compose exec postgres psql -U drh_user -d disaster_resilience -f /docker-entrypoint-initdb.d/01-schema.sql
   ```

6. **Access the application**
   - Frontend: http://localhost:3000
   - API Gateway: http://localhost:8080
   - API Documentation: http://localhost:8080/swagger-ui.html

7. **Default admin credentials**
   ```
   Username: admin
   Password: change_me_immediately
   ```
   **CHANGE IMMEDIATELY IN PRODUCTION!**

### Check Service Health

```bash
# Check all services
docker-compose ps

# View logs
docker-compose logs -f

# Check individual service health
curl http://localhost:8081/actuator/health  # User Session
curl http://localhost:8082/actuator/health  # Disaster Integrator
curl http://localhost:8000/health           # AI Model
```

---

## Configuration

### Environment Variables

Create a `.env` file in the root directory:

```env
# Database
POSTGRES_PASSWORD=changeme_secure_password
POSTGRES_USER=drh_user
POSTGRES_DB=disaster_resilience

# Redis
REDIS_PASSWORD=changeme_redis_password

# JWT Authentication
JWT_SECRET=changeme_jwt_secret_key_minimum_256_bits_for_hs256
JWT_EXPIRATION=3600000  # 1 hour in milliseconds

# LLM Service
LLM_API_KEY=your_xai_or_huggingface_api_key
LLM_PROVIDER=huggingface  # or 'xai'

# Spring Profile
SPRING_PROFILE=prod  # or 'dev'

# Logging
LOG_LEVEL=INFO  # or 'DEBUG'
```

### SSO Configuration (Enterprise)

To configure SSO providers:

1. **SAML 2.0**
   ```bash
   curl -X POST http://localhost:8081/api/sso/providers \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer YOUR_ADMIN_JWT" \
     -d '{
       "providerName": "OktaSAML",
       "providerType": "SAML",
       "entityId": "https://your-okta-domain.com",
       "ssoUrl": "https://your-okta-domain.com/sso/saml",
       "certificate": "YOUR_X509_CERTIFICATE"
     }'
   ```

2. **OAuth2/OIDC**
   ```bash
   curl -X POST http://localhost:8081/api/sso/providers \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer YOUR_ADMIN_JWT" \
     -d '{
       "providerName": "Azure AD",
       "providerType": "OAUTH2",
       "clientId": "your-client-id",
       "clientSecret": "your-client-secret",
       "authorizationEndpoint": "https://login.microsoftonline.com/...",
       "tokenEndpoint": "https://login.microsoftonline.com/..."
     }'
   ```

---

## API Documentation

### Authentication Endpoints

**POST /api/auth/register**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john.doe",
    "email": "john@example.com",
    "password": "SecurePass123!",
    "firstName": "John",
    "lastName": "Doe",
    "organization": "Red Cross"
  }'
```

**POST /api/auth/login**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john.doe",
    "password": "SecurePass123!"
  }'
```

Response:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": "uuid",
    "username": "john.doe",
    "email": "john@example.com",
    "role": "USER"
  }
}
```

### MFA Endpoints (Enterprise)

**POST /api/mfa/setup/totp**
```bash
curl -X POST http://localhost:8080/api/mfa/setup/totp \
  -H "Authorization: Bearer YOUR_JWT"
```

Response includes QR code for Google Authenticator/Authy.

**POST /api/mfa/verify**
```bash
curl -X POST http://localhost:8080/api/mfa/verify \
  -H "Authorization: Bearer YOUR_JWT" \
  -d '{
    "code": "123456"
  }'
```

### Disaster Data Endpoints

**POST /api/integrator/data/environmental/import**
```bash
curl -X POST http://localhost:8080/api/integrator/data/environmental/import \
  -H "Authorization: Bearer YOUR_JWT" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@noaa_weather_data.csv" \
  -F "source=NOAA" \
  -F "dataType=weather"
```

**GET /api/integrator/data/environmental/nearby**
```bash
curl "http://localhost:8080/api/integrator/data/environmental/nearby?latitude=40.7128&longitude=-74.0060&radiusKm=50" \
  -H "Authorization: Bearer YOUR_JWT"
```

### AI Prediction Endpoints

**POST /api/ai/predict/risk**
```bash
curl -X POST http://localhost:8000/predict/risk \
  -H "Content-Type: application/json" \
  -d '{
    "disaster_type": "flood",
    "environmental_data": {
      "latitude": 40.7128,
      "longitude": -74.0060,
      "temperature": 25,
      "precipitation": 50,
      "humidity": 80
    },
    "community_data": {
      "population_density": 5000,
      "available_shelters": 10
    },
    "individual_data": {
      "age": 35,
      "mobility_score": 8,
      "has_chronic_conditions": false
    }
  }'
```

Response:
```json
{
  "risk_level": "medium",
  "confidence_score": 0.82,
  "risk_score": 0.45,
  "recommendations": [
    "Stay informed about weather conditions",
    "Review your emergency plan",
    "Move to higher ground",
    "Avoid walking through flood waters"
  ]
}
```

[Full API Documentation](./docs/API.md)

---

## User Roles & Authentication

### Role Hierarchy

1. **USER** (Default)
   - View disaster data and predictions
   - Create personal evacuation plans
   - Access 3D visualizations
   - Use LLM queries
   - Join collaboration sessions

2. **MODERATOR**
   - All USER permissions
   - Manage community data
   - Verify user-submitted data
   - Moderate collaboration sessions
   - View system statistics

3. **ADMIN**
   - All MODERATOR permissions
   - Manage users and roles
   - Configure SSO providers (Enterprise)
   - Access audit logs
   - System configuration

### Account Types

- **STANDARD**: Basic features, community support
- **NONPROFIT**: All features free (requires verification)
- **ENTERPRISE**: SSO, MFA, priority support (requires commercial license)

---

## Enterprise Features

### Single Sign-On (SSO)

Supported protocols:
- SAML 2.0 (Okta, OneLogin, Azure AD)
- OAuth2/OIDC (Google, GitHub, Azure AD)
- Auto-user provisioning
- JIT (Just-In-Time) account creation

Configuration via Admin panel or API.

### Multi-Factor Authentication (MFA)

- **TOTP**: Google Authenticator, Authy, Microsoft Authenticator
- **SMS**: Integration with Twilio (requires setup)
- **Email**: Token-based verification
- **Backup Codes**: 10 single-use codes for recovery

Users can enable MFA in account settings.

---

## Development

### Local Development Setup

1. **Backend Services**
   ```bash
   # User Session
   cd backend/user-session
   mvn clean install
   mvn spring-boot:run

   # Disaster Integrator
   cd backend/disaster-integrator
   mvn clean install
   mvn spring-boot:run
   ```

2. **AI Model**
   ```bash
   cd ai-model
   pip install -r requirements.txt
   python disaster_predictor.py
   ```

3. **Frontend**
   ```bash
   cd frontend
   npm install
   npm start
   ```

### Project Structure

```
DisasterResilienceHub/
├── backend/
│   ├── user-session/          # Authentication & SSO
│   ├── disaster-integrator/   # Data ingestion
│   ├── disaster-visualizer/   # 3D visualizations
│   ├── llm-service/           # LLM queries
│   ├── collaboration-service/ # WebSocket collaboration
│   └── api-gateway/           # Routing & auth
├── frontend/                  # React + Three.js
├── ai-model/                  # Python/FastAPI/PyTorch
├── database/
│   ├── postgres/schema.sql    # Database schema
│   └── redis/config.yaml      # Redis configuration
├── infra/
│   ├── nginx/                 # Reverse proxy
│   └── kubernetes/            # K8s deployments
├── docker-compose.yml         # Docker orchestration
└── README.md
```

---

## Testing

### Run All Tests

```bash
# Backend services (JUnit)
cd backend/user-session
mvn test

cd backend/disaster-integrator
mvn test

# AI Model (Pytest)
cd ai-model
pytest

# Frontend (Jest)
cd frontend
npm test
```

### Integration Tests

```bash
# Uses Testcontainers for PostgreSQL
mvn verify -P integration-tests
```

### Test Coverage

Target: **>90%** code coverage across all services

```bash
# Generate coverage report
mvn jacoco:report  # Java
pytest --cov       # Python
npm run test:coverage  # JavaScript
```

---

## Deployment

### Production Deployment with Docker Compose

1. **Configure production environment**
   ```bash
   cp .env.example .env.production
   # Edit with production values
   ```

2. **Build and deploy**
   ```bash
   docker-compose -f docker-compose.yml --env-file .env.production up -d
   ```

3. **Setup SSL/TLS**
   - Add certificates to `infra/nginx/ssl/`
   - Update `infra/nginx/default.conf`

### Kubernetes Deployment

```bash
# Apply configurations
kubectl apply -f infra/kubernetes/

# Check status
kubectl get pods -n disaster-resilience

# Access via LoadBalancer
kubectl get svc -n disaster-resilience
```

### Scaling

```bash
# Scale specific services
docker-compose up -d --scale disaster-integrator=3

# Kubernetes
kubectl scale deployment disaster-integrator --replicas=3
```

---

## Contributing

We welcome contributions! Please see [CONTRIBUTING.md](./CONTRIBUTING.md) for guidelines.

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## Support

### Community Support (Open Source Users)
- GitHub Issues: https://github.com/your-org/DisasterResilienceHub/issues
- Documentation: https://docs.disasterresiliencehub.org
- Community Forum: https://community.disasterresiliencehub.org

### Enterprise Support (Commercial License)
- Email: support@disasterresiliencehub.org
- Phone: +1-XXX-XXX-XXXX
- SLA-based priority support
- Dedicated technical account manager

### Security Issues
Report security vulnerabilities to: security@disasterresiliencehub.org

---

## Acknowledgments

- **NOAA** for weather data
- **USGS** for seismic data
- **OpenStreetMap** for community infrastructure data
- **FHIR** for health record standards
- All open-source contributors

---

## License

This project is dual-licensed:
- [Apache 2.0](./LICENSE-OPEN-SOURCE.md) for non-profit use
- [Commercial License](./LICENSE-COMMERCIAL.md) for for-profit use

See [LICENSE](./LICENSE) for details.

---

## Mission

**DisasterResilienceHub is committed to advancing global disaster resilience by making preparedness tools accessible to all while building a sustainable platform for continued development.**

Together, we can save lives and build resilient communities.

---

**Made with care for humanity**

Copyright © 2025 DisasterResilienceHub
