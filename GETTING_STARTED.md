# Getting Started with DisasterResilienceHub

Welcome! This guide will help you get DisasterResilienceHub up and running quickly.

---

## Prerequisites

Before you begin, ensure you have the following installed:

- [Docker](https://www.docker.com/get-started) (v20.10+)
- [Docker Compose](https://docs.docker.com/compose/install/) (v2.0+)
- [Git](https://git-scm.com/downloads)

**Optional (for local development):**
- Java 17 (OpenJDK)
- Node.js 18+
- Python 3.10+
- Maven 3.8+

---

## Quick Start (5 Minutes)

### 1. Clone the Repository

```bash
git clone https://github.com/sekacorn/Disaster-Resileince-Hub.git
cd DisasterResilienceHub
```

### 2. Configure Environment

```bash
cp .env.example .env
```

**Edit `.env` with your configuration:**

```env
# Required - Change these immediately!
POSTGRES_PASSWORD=your_secure_password_here
REDIS_PASSWORD=your_redis_password_here
JWT_SECRET=your_jwt_secret_minimum_256_bits_here

# Optional - For LLM features
LLM_PROVIDER=huggingface  # or 'xai'
LLM_API_KEY=your_api_key_here
```

**Important Security Notes:**
- `POSTGRES_PASSWORD` - Use a strong password (16+ characters)
- `REDIS_PASSWORD` - Use a strong password (16+ characters)
- `JWT_SECRET` - Must be at least 256 bits (32 characters) for HS256

### 3. Start All Services

```bash
docker-compose up -d
```

This will start:
- PostgreSQL database (Port 5432)
- Redis cache (Port 6379)
- User Session service (Port 8081)
- Disaster Integrator service (Port 8082)
- AI Model service (Port 8000)
- NGINX reverse proxy (Port 80/443)

### 4. Verify Services Are Running

```bash
docker-compose ps
```

All services should show `Up` status.

### 5. Check Health

```bash
# User Session Service
curl http://localhost:8081/actuator/health

# Disaster Integrator Service
curl http://localhost:8082/actuator/health

# AI Model Service
curl http://localhost:8000/health
```

All should return `{"status":"UP"}` or similar.

### 6. Access the Application

- **API Gateway**: http://localhost:8080 *(when implemented)*
- **Frontend**: http://localhost:3000 *(when implemented)*
- **Direct API Access**: http://localhost:8081 (User Session)

---

## First Steps After Installation

### 1. Change Default Admin Password

The system comes with a default admin account:

**Default Credentials:**
- Username: `admin`
- Password: `change_me_immediately`

**CHANGE THIS IMMEDIATELY!**

```bash
# Login to get JWT token
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "change_me_immediately"
  }'

# Use the returned token to change password
curl -X PUT http://localhost:8081/api/users/{userId}/password \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "currentPassword": "change_me_immediately",
    "newPassword": "YourNewSecurePassword123!"
  }'
```

### 2. Create Your First User Account

```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john.doe",
    "email": "john@example.com",
    "password": "SecurePass123!",
    "firstName": "John",
    "lastName": "Doe",
    "organization": "Your Organization"
  }'
```

### 3. Login and Get JWT Token

```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john.doe",
    "password": "SecurePass123!"
  }'
```

Save the `accessToken` from the response - you'll need it for authenticated requests.

---

## Testing the System

### 1. Upload Environmental Data

```bash
# Create a test CSV file
cat > test_weather.csv << EOF
latitude,longitude,temperature,humidity,wind_speed,precipitation
40.7128,-74.0060,25.5,65,15.2,5.0
34.0522,-118.2437,28.0,45,10.5,0.0
EOF

# Upload it
curl -X POST http://localhost:8082/api/integrator/data/environmental/import \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@test_weather.csv" \
  -F "source=NOAA" \
  -F "dataType=weather"
```

### 2. Get Risk Prediction

```bash
curl -X POST http://localhost:8000/predict/risk \
  -H "Content-Type: application/json" \
  -d '{
    "disaster_type": "flood",
    "environmental_data": {
      "latitude": 40.7128,
      "longitude": -74.0060,
      "temperature": 25.5,
      "precipitation": 50,
      "humidity": 80
    }
  }'
```

### 3. Get Evacuation Plan

```bash
curl -X POST http://localhost:8000/predict/evacuation \
  -H "Content-Type: application/json" \
  -d '{
    "start_latitude": 40.7128,
    "start_longitude": -74.0060,
    "disaster_type": "flood",
    "environmental_data": {
      "latitude": 40.7128,
      "longitude": -74.0060,
      "precipitation": 50
    },
    "individual_data": {
      "age": 35,
      "mobility_score": 8
    }
  }'
```

---

## Enabling Enterprise Features

### SSO Configuration (Enterprise License Required)

1. **Configure SSO Provider (Admin only)**

```bash
curl -X POST http://localhost:8081/api/sso/providers \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "providerName": "Okta",
    "providerType": "SAML",
    "entityId": "https://your-okta-domain.okta.com",
    "ssoUrl": "https://your-okta-domain.okta.com/app/your-app/sso/saml",
    "certificate": "YOUR_X509_CERTIFICATE",
    "isActive": true,
    "autoProvisionUsers": true,
    "defaultRole": "USER"
  }'
```

2. **Users can now login via SSO**

```bash
curl -X POST http://localhost:8081/api/sso/auth/Okta \
  -H "Content-Type: application/json" \
  -d '{
    "samlResponse": "BASE64_ENCODED_SAML_RESPONSE"
  }'
```

### MFA Setup (Enterprise License Required)

1. **Enable TOTP MFA**

```bash
curl -X POST http://localhost:8081/api/mfa/setup/totp \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

Response will include:
- `secret` - Secret key for manual entry
- `qrCodeUrl` - QR code image URL (scan with Google Authenticator/Authy)
- `backupCodes` - 10 backup codes (save these securely!)

2. **Verify MFA Setup**

```bash
curl -X POST http://localhost:8081/api/mfa/verify \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "123456"
  }'
```

3. **Login with MFA**

```bash
# Step 1: Login (will require MFA)
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john.doe",
    "password": "SecurePass123!",
    "mfaCode": "123456"
  }'
```

---

## User Roles & Permissions

### USER (Default Role)
- View disaster data and predictions
- Create personal evacuation plans
- Access 3D visualizations
- Use LLM queries
- Join collaboration sessions

### MODERATOR
- All USER permissions +
- Manage community data
- Verify user-submitted data
- Moderate collaboration sessions
- View system statistics

### ADMIN
- All MODERATOR permissions +
- Manage users and assign roles
- Configure SSO providers
- Access audit logs
- System configuration

### Changing User Roles (Admin Only)

```bash
curl -X PUT http://localhost:8081/api/users/{userId}/role \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "role": "MODERATOR"
  }'
```

---

## Viewing Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f user-session
docker-compose logs -f disaster-integrator
docker-compose logs -f ai-model

# Last 100 lines
docker-compose logs --tail=100 user-session
```

---

## Stopping and Restarting

### Stop All Services

```bash
docker-compose down
```

### Stop and Remove Volumes (Complete Reset)

```bash
docker-compose down -v
```

**Warning:** This will delete all data including users, disaster data, and configurations!

### Restart Services

```bash
docker-compose restart
```

### Restart Specific Service

```bash
docker-compose restart user-session
```

---

## Troubleshooting

### Services Won't Start

**Check logs:**
```bash
docker-compose logs
```

**Common issues:**
- Port conflicts (8080, 8081, 8082, 5432, 6379 already in use)
- Insufficient memory (requires ~4GB RAM)
- Missing `.env` file

### Database Connection Issues

```bash
# Check PostgreSQL is running
docker-compose ps postgres

# Connect to database
docker-compose exec postgres psql -U drh_user -d disaster_resilience

# Check tables
\dt
```

### Redis Connection Issues

```bash
# Check Redis is running
docker-compose ps redis

# Connect to Redis
docker-compose exec redis redis-cli -a YOUR_REDIS_PASSWORD

# Test connection
PING
```

### Cannot Login / JWT Issues

**Ensure JWT_SECRET is properly configured:**
- Must be at least 256 bits (32 characters)
- Should be complex and random
- Must be the same across all services

**Check user exists:**
```bash
docker-compose exec postgres psql -U drh_user -d disaster_resilience -c "SELECT username, email, role FROM users;"
```

---

## Next Steps

1. **Explore the API** - See [README.md](./README.md) for complete API documentation
2. **Read the Project Status** - See [PROJECT_STATUS.md](./PROJECT_STATUS.md) for implementation details
3. **Configure for Production** - See deployment section in README.md
4. **Set up Monitoring** - Configure Prometheus and Grafana
5. **Review Security** - Audit settings for production use

---

## Getting Help

### Community Support (Open Source)
- GitHub Issues: https://github.com/sekacorn/Disaster-Resileince-Hub.gi
- Documentation: [README.md](./README.md)

### Enterprise Support (Commercial License)
- Email: support@disasterresiliencehub.org
- Priority technical support with SLA

### Security Issues
- Email: security@disasterresiliencehub.org

---

## Licensing

Choose the appropriate license:

**Non-Profit / Educational / Humanitarian:**
- FREE under Apache 2.0 License
- See [LICENSE-OPEN-SOURCE.md](./LICENSE-OPEN-SOURCE.md)

**For-Profit / Commercial:**
- 4% of gross revenue
- Includes enterprise features (SSO, MFA)
- See [LICENSE-COMMERCIAL.md](./LICENSE-COMMERCIAL.md)
- Contact: licensing@disasterresiliencehub.org

---

**Ready to save lives and build resilient communities!**

Copyright © 2025 DisasterResilienceHub
