# User Session Microservice

## Overview

The User Session microservice handles authentication, authorization, user management, SSO integration, and multi-factor authentication for the DisasterResilienceHub platform.

## Features

### Core Authentication & Authorization
- **Username/Password Authentication**: Standard BCrypt-hashed password authentication
- **JWT Token Management**: Access and refresh token generation and validation
- **Role-Based Access Control (RBAC)**: Support for USER, MODERATOR, and ADMIN roles
- **Session Management**: Redis-backed session storage with PostgreSQL persistence

### Enterprise Features

#### SSO Integration
- **SAML 2.0 Support**: Enterprise single sign-on with SAML providers
- **OAuth2/OIDC Support**: Integration with OAuth2 and OpenID Connect providers
- **Auto-Provisioning**: Automatic user creation from SSO attributes
- **Multiple Providers**: Support for multiple SSO providers simultaneously

#### Multi-Factor Authentication (MFA)
- **TOTP Support**: Time-based one-time passwords (Google Authenticator, Authy)
- **QR Code Generation**: Easy setup with authenticator apps
- **Backup Codes**: Emergency access codes for account recovery
- **SMS/Email MFA**: Placeholder for future SMS and email MFA integration

### User Management
- **User Registration**: Self-service account creation with email verification
- **Profile Management**: Update user information and preferences
- **Password Management**: Change password and reset workflows
- **Account Locking**: Automatic locking after failed login attempts
- **Account Administration**: Admin tools for user management

### Security Features
- **Rate Limiting**: Protection against brute-force attacks
- **Account Locking**: Automatic lockout after failed attempts
- **Session Tracking**: IP address and user agent logging
- **Audit Logging**: Security event tracking
- **Input Validation**: Comprehensive validation of all inputs

## Technology Stack

- **Java 17**: Modern Java with latest features
- **Spring Boot 3.2.1**: Core framework
- **Spring Security**: Authentication and authorization
- **Spring Data JPA**: Database access
- **PostgreSQL**: Persistent storage
- **Redis**: Session caching
- **JWT (jjwt 0.12.3)**: Token generation and validation
- **Google Authenticator**: TOTP implementation
- **ZXing**: QR code generation
- **Testcontainers**: Integration testing

## Project Structure

```
user-session/
├── src/
│   ├── main/
│   │   ├── java/com/disaster/session/
│   │   │   ├── UserSessionApp.java          # Main application
│   │   │   ├── config/                      # Configuration
│   │   │   │   └── ApplicationConfig.java
│   │   │   ├── controller/                  # REST controllers
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── UserController.java
│   │   │   │   ├── MfaController.java
│   │   │   │   └── SsoController.java
│   │   │   ├── dto/                         # Data transfer objects
│   │   │   │   ├── LoginRequest.java
│   │   │   │   ├── LoginResponse.java
│   │   │   │   ├── RegisterRequest.java
│   │   │   │   ├── UserDto.java
│   │   │   │   └── MfaSetupResponse.java
│   │   │   ├── exception/                   # Exception handling
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   ├── model/                       # JPA entities
│   │   │   │   ├── User.java
│   │   │   │   ├── UserRole.java
│   │   │   │   ├── UserSession.java
│   │   │   │   ├── MfaVerification.java
│   │   │   │   ├── SsoProvider.java
│   │   │   │   ├── AccountType.java
│   │   │   │   └── MfaType.java
│   │   │   ├── repository/                  # Data repositories
│   │   │   │   ├── UserRepository.java
│   │   │   │   ├── SessionRepository.java
│   │   │   │   ├── SsoProviderRepository.java
│   │   │   │   └── MfaVerificationRepository.java
│   │   │   ├── security/                    # Security configuration
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   └── CustomUserDetailsService.java
│   │   │   └── service/                     # Business logic
│   │   │       ├── AuthService.java
│   │   │       ├── UserService.java
│   │   │       ├── JwtService.java
│   │   │       ├── MfaService.java
│   │   │       ├── SsoService.java
│   │   │       └── ScheduledTaskService.java
│   │   └── resources/
│   │       └── application.yml              # Configuration
│   └── test/
│       └── java/com/disaster/session/       # Tests
│           ├── service/
│           │   ├── JwtServiceTest.java
│           │   └── AuthServiceTest.java
│           ├── controller/
│           │   └── AuthControllerTest.java
│           └── integration/
│               └── UserSessionIntegrationTest.java
└── pom.xml                                  # Maven dependencies
```

## API Endpoints

### Authentication Endpoints

#### POST /api/auth/login
Login with username/email and password.

**Request:**
```json
{
  "usernameOrEmail": "user@example.com",
  "password": "password123",
  "mfaCode": "123456",  // Optional, required if MFA is enabled
  "rememberMe": false
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": { ... },
  "mfaRequired": false
}
```

#### POST /api/auth/register
Register a new user account.

**Request:**
```json
{
  "username": "newuser",
  "email": "newuser@example.com",
  "password": "SecurePassword123!",
  "firstName": "John",
  "lastName": "Doe",
  "phoneNumber": "+1234567890",
  "organization": "Example Org"
}
```

#### POST /api/auth/refresh
Refresh an access token.

**Request:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

#### POST /api/auth/logout
Logout and invalidate the current session.

#### POST /api/auth/forgot-password
Initiate password reset process.

#### POST /api/auth/reset-password
Reset password with a reset token.

### User Management Endpoints

#### GET /api/users/{userId}
Get user details (requires authentication).

#### GET /api/users
Get all users (Admin only).

#### PUT /api/users/{userId}
Update user profile.

#### PUT /api/users/{userId}/password
Change user password.

#### PUT /api/users/{userId}/role
Update user role (Admin only).

#### PUT /api/users/{userId}/activate
Activate user account (Admin only).

#### PUT /api/users/{userId}/deactivate
Deactivate user account (Admin only).

#### DELETE /api/users/{userId}
Delete user account (Admin only).

### MFA Endpoints

#### POST /api/mfa/setup/totp
Setup TOTP-based MFA.

**Response:**
```json
{
  "secret": "JBSWY3DPEHPK3PXP",
  "qrCodeDataUri": "data:image/png;base64,...",
  "otpauthUrl": "otpauth://totp/...",
  "backupCodes": ["12345678", "87654321", ...],
  "message": "Scan the QR code with your authenticator app"
}
```

#### POST /api/mfa/verify/totp
Verify and enable TOTP MFA.

#### POST /api/mfa/verify
Verify MFA code.

#### POST /api/mfa/disable
Disable MFA for the user.

### SSO Endpoints

#### POST /api/sso/providers
Create SSO provider configuration (Admin only).

#### GET /api/sso/providers/{providerId}
Get SSO provider details (Admin only).

#### GET /api/sso/providers
Get all active SSO providers.

#### PUT /api/sso/providers/{providerId}
Update SSO provider (Admin only).

#### DELETE /api/sso/providers/{providerId}
Delete SSO provider (Admin only).

#### POST /api/sso/auth/{providerName}
Initiate SSO authentication.

#### POST /api/sso/callback/{providerName}
Handle SSO callback.

## Configuration

### Environment Variables

```bash
# Database
DB_USERNAME=postgres
DB_PASSWORD=your_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# JWT
JWT_SECRET=your_base64_encoded_secret
JWT_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=604800000

# Authentication
MAX_FAILED_ATTEMPTS=5
LOCK_DURATION_MINUTES=30

# Server
SERVER_PORT=8081

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173

# SSO Providers (if using)
GOOGLE_CLIENT_ID=your_client_id
GOOGLE_CLIENT_SECRET=your_client_secret
AZURE_CLIENT_ID=your_client_id
AZURE_CLIENT_SECRET=your_client_secret
```

### Application Profiles

- **dev**: Development profile with detailed logging and auto DDL updates
- **prod**: Production profile with minimal logging and strict security

## Running the Service

### Prerequisites

1. Java 17 or higher
2. PostgreSQL 15 or higher
3. Redis 7 or higher
4. Maven 3.8 or higher

### Setup Database

Run the schema script:
```bash
psql -U postgres -d disaster_resilience_hub -f database/postgres/schema.sql
```

### Build and Run

```bash
# Build
mvn clean install

# Run with dev profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Run with prod profile
mvn spring-boot:run -Dspring-boot.run.profiles=prod

# Run JAR
java -jar target/user-session-1.0.0.jar
```

### Docker

```bash
# Build image
docker build -t disaster-hub/user-session:1.0.0 .

# Run container
docker run -p 8081:8081 \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=password \
  -e REDIS_HOST=redis \
  disaster-hub/user-session:1.0.0
```

## Testing

### Run Unit Tests

```bash
mvn test
```

### Run Integration Tests

```bash
mvn verify
```

### Run Specific Test

```bash
mvn test -Dtest=AuthServiceTest
```

## Security Best Practices

1. **JWT Secret**: Use a strong, randomly generated secret (at least 256 bits)
2. **Password Policy**: Enforce strong passwords (minimum 8 characters)
3. **HTTPS**: Always use HTTPS in production
4. **Rate Limiting**: Configure appropriate rate limits
5. **Token Expiration**: Keep access token expiration short (1 hour recommended)
6. **MFA**: Enable MFA for admin and moderator accounts
7. **Audit Logging**: Monitor audit logs for suspicious activity

## Monitoring

### Health Check

```bash
curl http://localhost:8081/actuator/health
```

### Metrics

```bash
curl http://localhost:8081/actuator/metrics
```

### Prometheus

Metrics are exposed at `/actuator/prometheus` for Prometheus scraping.

## Scheduled Tasks

- **Session Cleanup**: Every hour (0 0 * * * *)
- **MFA Verification Cleanup**: Every 15 minutes (0 */15 * * * *)
- **Account Unlock**: Every 10 minutes (0 */10 * * * *)

## Troubleshooting

### Common Issues

1. **Connection refused to PostgreSQL**: Check database is running and credentials are correct
2. **Redis connection timeout**: Verify Redis is running and accessible
3. **JWT validation errors**: Ensure JWT secret matches between services
4. **Account locked**: Wait for lock duration or have admin unlock account

## Contributing

1. Follow Java coding conventions
2. Write comprehensive tests for new features
3. Update documentation for API changes
4. Use meaningful commit messages

## License

Proprietary - DisasterResilienceHub

## Support

For issues or questions, contact the development team.
