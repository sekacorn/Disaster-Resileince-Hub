# Disaster Integrator - Quick Start Guide

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL 14+
- Git (optional)

## 1. Database Setup

### Install PostgreSQL
Download and install PostgreSQL from https://www.postgresql.org/download/

### Create Database
```sql
-- Connect to PostgreSQL
psql -U postgres

-- Create database
CREATE DATABASE disaster_resilience;

-- Create user (optional)
CREATE USER disaster_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE disaster_resilience TO disaster_user;
```

## 2. Configure Application

Edit `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/disaster_resilience
    username: postgres  # or disaster_user
    password: your_password
```

### Set JWT Secret (Optional)
```yaml
security:
  jwt:
    secret: your-secret-key-min-256-bits
```

Or use environment variable:
```bash
export JWT_SECRET=your-secret-key-min-256-bits
```

## 3. Build the Project

```bash
# Navigate to project directory
cd backend/disaster-integrator

# Clean and build
mvn clean install

# Skip tests for faster build (not recommended)
mvn clean install -DskipTests
```

## 4. Run the Application

### Option 1: Using Maven
```bash
mvn spring-boot:run
```

### Option 2: Using Java
```bash
# After mvn clean install
java -jar target/disaster-integrator-1.0.0.jar
```

### Option 3: With Custom Port
```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8083
```

## 5. Verify Installation

### Check Health Endpoint
```bash
curl http://localhost:8082/api/integrator/data/health
```

Expected response:
```json
{
  "status": "UP",
  "service": "Disaster Integrator",
  "timestamp": "2025-01-15T12:00:00"
}
```

## 6. Get JWT Token

You'll need a JWT token to access protected endpoints. The token should be obtained from the authentication service. For testing, you can use a test token generator or mock authentication.

### Example JWT Token (for testing only)
```bash
# Set your token
export JWT_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

## 7. Test API Endpoints

### Get Statistics
```bash
curl -X GET http://localhost:8082/api/integrator/data/statistics \
  -H "Authorization: Bearer $JWT_TOKEN"
```

### Import Sample NOAA Data

Create a sample CSV file `weather_sample.csv`:
```csv
timestamp,latitude,longitude,temperature,humidity,wind_speed,wind_direction,precipitation,pressure,condition
2025-01-15 12:00:00,40.7128,-74.0060,15.5,65,8.5,NW,0.0,1013.25,Clear
2025-01-15 12:00:00,34.0522,-118.2437,22.0,45,5.2,W,0.0,1015.50,Sunny
```

Import the data:
```bash
curl -X POST http://localhost:8082/api/integrator/data/environmental/noaa/import \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -F "file=@weather_sample.csv"
```

### Query Environmental Data
```bash
curl -X GET "http://localhost:8082/api/integrator/data/environmental/radius?latitude=40.7128&longitude=-74.0060&radiusKm=100" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

## 8. Sample Data Files

### USGS Seismic Data (earthquake_sample.json)
```json
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "properties": {
        "mag": 4.5,
        "place": "10km NE of Los Angeles",
        "time": 1642248000000,
        "type": "earthquake",
        "status": "reviewed"
      },
      "geometry": {
        "type": "Point",
        "coordinates": [-118.2437, 34.0522, 10.0]
      }
    }
  ]
}
```

Import:
```bash
curl -X POST http://localhost:8082/api/integrator/data/environmental/usgs/import \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -F "file=@earthquake_sample.json"
```

### OpenStreetMap Community Data (facilities_sample.geojson)
```json
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "properties": {
        "name": "Central Hospital",
        "amenity": "hospital",
        "capacity": 500,
        "phone": "+1234567890",
        "wheelchair": "yes",
        "addr:street": "123 Main St",
        "addr:city": "New York",
        "addr:state": "NY"
      },
      "geometry": {
        "type": "Point",
        "coordinates": [-74.0060, 40.7128]
      }
    }
  ]
}
```

Import:
```bash
curl -X POST http://localhost:8082/api/integrator/data/community/osm/import \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -F "file=@facilities_sample.geojson"
```

### FHIR Patient Data (patient_sample.json)
```json
{
  "resourceType": "Patient",
  "id": "patient123",
  "name": [
    {
      "given": ["John"],
      "family": "Doe"
    }
  ],
  "birthDate": "1980-01-01",
  "gender": "male",
  "telecom": [
    {
      "system": "phone",
      "value": "+1234567890"
    },
    {
      "system": "email",
      "value": "john.doe@example.com"
    }
  ]
}
```

Import:
```bash
curl -X POST http://localhost:8082/api/integrator/data/health/fhir/patient \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -F "file=@patient_sample.json"
```

## 9. Common Operations

### Find Nearby Hospitals
```bash
curl -X GET "http://localhost:8082/api/integrator/data/community/hospitals/nearby?latitude=40.7128&longitude=-74.0060&radiusKm=25" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

### Find Available Shelters
```bash
curl -X GET http://localhost:8082/api/integrator/data/community/shelters/available \
  -H "Authorization: Bearer $JWT_TOKEN"
```

### Get High Severity Events
```bash
curl -X GET "http://localhost:8082/api/integrator/data/environmental/high-severity?hoursAgo=24" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

### Get My Health Data
```bash
curl -X GET http://localhost:8082/api/integrator/data/health/me \
  -H "Authorization: Bearer $JWT_TOKEN"
```

## 10. Run Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=EnvironmentalDataServiceTest

# Run tests with coverage
mvn test jacoco:report
```

## 11. Troubleshooting

### Database Connection Issues
```bash
# Check PostgreSQL is running
sudo systemctl status postgresql  # Linux
pg_ctl status                      # Windows

# Test connection
psql -U postgres -d disaster_resilience -c "SELECT 1;"
```

### Port Already in Use
```bash
# Change port in application.yml
server:
  port: 8083

# Or use environment variable
export SERVER_PORT=8083
mvn spring-boot:run
```

### JWT Token Issues
- Ensure JWT secret is at least 256 bits (32 characters)
- Check token expiration
- Verify token format: "Bearer {token}"

### Out of Memory
```bash
# Increase heap size
export MAVEN_OPTS="-Xmx1024m"
mvn spring-boot:run
```

## 12. Development Tips

### Hot Reload (Spring DevTools)
Add to pom.xml:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <optional>true</optional>
</dependency>
```

### View SQL Queries
Set in application.yml:
```yaml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
```

### Enable Debug Logging
```yaml
logging:
  level:
    com.disaster.integrator: DEBUG
```

## 13. Production Deployment

### Build for Production
```bash
mvn clean package -Pprod -DskipTests
```

### Run with Production Profile
```bash
java -jar target/disaster-integrator-1.0.0.jar --spring.profiles.active=prod
```

### Environment Variables
```bash
export DB_USERNAME=prod_user
export DB_PASSWORD=prod_password
export JWT_SECRET=production-secret-key-min-256-bits
java -jar target/disaster-integrator-1.0.0.jar
```

### Docker Deployment (Optional)
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/disaster-integrator-1.0.0.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

Build and run:
```bash
docker build -t disaster-integrator .
docker run -p 8082:8082 -e DB_USERNAME=postgres -e DB_PASSWORD=password disaster-integrator
```

## 14. Next Steps

1. **Integrate with Auth Service**: Set up proper JWT token generation
2. **Import Real Data**: Use actual NOAA, USGS, OSM data
3. **Set Up Monitoring**: Configure actuator endpoints
4. **Add API Documentation**: Integrate Swagger/OpenAPI
5. **Configure CI/CD**: Set up automated testing and deployment
6. **Scale Horizontally**: Deploy multiple instances with load balancer

## 15. Support & Resources

- **API Documentation**: See README.md
- **Architecture**: See PROJECT_SUMMARY.md
- **Code Examples**: Check test classes
- **Issues**: Report bugs in issue tracker
- **Community**: Join development team

## Quick Command Reference

```bash
# Build
mvn clean install

# Run
mvn spring-boot:run

# Test
mvn test

# Package
mvn package

# Health Check
curl http://localhost:8082/api/integrator/data/health

# Statistics
curl -H "Authorization: Bearer $JWT_TOKEN" \
  http://localhost:8082/api/integrator/data/statistics
```

---

**Congratulations!** You now have the Disaster Integrator microservice running. Start importing data and building disaster resilience solutions!
