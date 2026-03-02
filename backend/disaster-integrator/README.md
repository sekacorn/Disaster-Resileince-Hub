# Disaster Integrator Microservice

## Overview

The Disaster Integrator microservice is a core component of the Disaster Resilience Hub platform. It integrates real-time environmental, community, and individual data from multiple sources to provide comprehensive disaster preparedness and response capabilities.

## Features

### 1. Data Integration
- **NOAA Weather Data**: Import and process weather data from CSV files
- **USGS Seismic Data**: Import and process earthquake data from JSON files
- **OpenStreetMap Community Data**: Import community facilities from GeoJSON files
- **FHIR Health Records**: Import FHIR-compliant health records (R4 standard)

### 2. Data Validation
- Comprehensive validation for all data types
- Geographic coordinate validation
- Range validation for numerical values
- Format validation for emails and phone numbers

### 3. Data Storage
- PostgreSQL database for persistent storage
- Optimized indexes for geographic queries
- JPA/Hibernate for data access

### 4. REST APIs
- Environmental data upload and retrieval
- Community facility management
- Health record management
- Geographic radius-based queries

### 5. Security
- JWT-based authentication
- Stateless session management
- Role-based access control

## Technology Stack

- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Data JPA**
- **Spring Security**
- **PostgreSQL**
- **HAPI FHIR 6.10.0**
- **Apache Commons CSV**
- **Jackson JSON**
- **Lombok**
- **JUnit 5 & Mockito**

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL 14+

## Configuration

### Database Setup

1. Create PostgreSQL database:
```sql
CREATE DATABASE disaster_resilience;
```

2. Update `application.yml` with your database credentials:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/disaster_resilience
    username: your_username
    password: your_password
```

### JWT Configuration

Set your JWT secret in `application.yml` or as an environment variable:
```yaml
security:
  jwt:
    secret: ${JWT_SECRET:your-secret-key}
```

## Building the Project

```bash
mvn clean install
```

## Running the Application

```bash
mvn spring-boot:run
```

The service will start on port 8082 by default: `http://localhost:8082/api/integrator`

## API Endpoints

### Environmental Data

#### Import NOAA Weather Data
```http
POST /data/environmental/noaa/import
Content-Type: multipart/form-data
Authorization: Bearer {token}

file: weather_data.csv
```

#### Import USGS Seismic Data
```http
POST /data/environmental/usgs/import
Content-Type: multipart/form-data
Authorization: Bearer {token}

file: earthquake_data.json
```

#### Get Environmental Data Within Radius
```http
GET /data/environmental/radius?latitude=40.7128&longitude=-74.0060&radiusKm=50
Authorization: Bearer {token}
```

#### Get High Severity Events
```http
GET /data/environmental/high-severity?hoursAgo=24
Authorization: Bearer {token}
```

### Community Data

#### Import OpenStreetMap Data
```http
POST /data/community/osm/import
Content-Type: multipart/form-data
Authorization: Bearer {token}

file: facilities.geojson
```

#### Get Community Facilities Within Radius
```http
GET /data/community/radius?latitude=40.7128&longitude=-74.0060&radiusKm=50
Authorization: Bearer {token}
```

#### Get Available Shelters
```http
GET /data/community/shelters/available
Authorization: Bearer {token}
```

#### Get Nearby Hospitals
```http
GET /data/community/hospitals/nearby?latitude=40.7128&longitude=-74.0060&radiusKm=25
Authorization: Bearer {token}
```

### Health Data

#### Import FHIR Patient Data
```http
POST /data/health/fhir/patient
Content-Type: multipart/form-data
Authorization: Bearer {token}

file: patient.json
```

#### Import FHIR Bundle Data
```http
POST /data/health/fhir/bundle
Content-Type: multipart/form-data
Authorization: Bearer {token}

file: bundle.json
```

#### Get My Health Data
```http
GET /data/health/me
Authorization: Bearer {token}
```

#### Update Consent
```http
PUT /data/health/consent?consentGiven=true
Authorization: Bearer {token}
```

### Statistics

#### Get Data Statistics
```http
GET /data/statistics
Authorization: Bearer {token}
```

Response:
```json
{
  "environmentalData": {
    "total": 1500,
    "weather": 1200,
    "seismic": 300
  },
  "communityData": {
    "total": 500,
    "hospitals": 50,
    "shelters": 100
  },
  "healthData": {
    "total": 1000,
    "highRisk": 150,
    "critical": 25
  }
}
```

## Data Formats

### NOAA Weather CSV Format
```csv
timestamp,latitude,longitude,temperature,humidity,wind_speed,wind_direction,precipitation,pressure,condition
2025-01-15 12:00:00,40.7128,-74.0060,15.5,65,8.5,NW,0.0,1013.25,Clear
```

### USGS Seismic JSON Format (GeoJSON)
```json
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "properties": {
        "mag": 4.5,
        "place": "10km NE of City",
        "time": 1642248000000,
        "type": "earthquake",
        "status": "reviewed"
      },
      "geometry": {
        "type": "Point",
        "coordinates": [-74.0060, 40.7128, 10.0]
      }
    }
  ]
}
```

### OpenStreetMap GeoJSON Format
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
        "wheelchair": "yes"
      },
      "geometry": {
        "type": "Point",
        "coordinates": [-74.0060, 40.7128]
      }
    }
  ]
}
```

### FHIR Patient JSON Format (R4)
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

## Testing

### Run All Tests
```bash
mvn test
```

### Run Specific Test
```bash
mvn test -Dtest=EnvironmentalDataServiceTest
```

### Test Coverage
The project includes comprehensive unit tests for:
- Service layer (EnvironmentalDataService, CommunityDataService, HealthDataService)
- Utility classes (DataValidator)
- Repository queries
- Data validation logic

## Database Schema

### Environmental Data Table
- id (Primary Key)
- source (NOAA, USGS)
- data_type (WEATHER, SEISMIC)
- timestamp
- latitude, longitude
- Weather fields: temperature, humidity, wind_speed, etc.
- Seismic fields: magnitude, depth, seismic_type
- severity, alert_level
- created_at, updated_at

### Community Data Table
- id (Primary Key)
- source (OSM, COMMUNITY_REPORT)
- facility_type (HOSPITAL, SHELTER, etc.)
- name, address, city, state
- latitude, longitude
- capacity, current_occupancy
- operational_status
- Resource flags: has_medical_supplies, has_food, etc.
- created_at, updated_at

### Individual Health Data Table
- id (Primary Key)
- user_id, patient_id, fhir_resource_id
- first_name, last_name
- date_of_birth, gender, blood_type
- medical_conditions, allergies, medications
- Special needs flags
- risk_level, risk_factors
- consent_given, data_encrypted
- created_at, updated_at

### User Locations Table
- id (Primary Key)
- user_id
- latitude, longitude, altitude
- timestamp, accuracy
- location_type, is_primary
- alert settings
- created_at, updated_at

## Error Handling

The service provides detailed error messages for:
- Validation failures
- Missing required fields
- Invalid data formats
- Authentication/authorization errors
- Database errors

Example error response:
```json
{
  "error": "Validation failed: Invalid latitude: 95.0, Temperature out of valid range: 100.0"
}
```

## Performance Considerations

- Geographic queries use optimized Haversine formula
- Batch processing for bulk imports
- Database connection pooling (HikariCP)
- Indexed columns for fast queries
- File size limits: 50MB per upload

## Security Considerations

- All endpoints require JWT authentication
- Health data is encrypted at rest
- Consent management for health records
- Role-based access for sensitive data
- Input validation and sanitization

## Monitoring

Health check endpoint:
```http
GET /data/health
```

Response:
```json
{
  "status": "UP",
  "service": "Disaster Integrator",
  "timestamp": "2025-01-15T12:00:00"
}
```

## Future Enhancements

- Real-time data streaming
- WebSocket support for live updates
- Machine learning for risk prediction
- Multi-language support
- Enhanced geographic querying with PostGIS
- Data export capabilities
- Integration with more data sources

## Contributing

Please follow the coding standards and include tests for new features.

## License

Copyright 2025 Disaster Resilience Hub Team

## Contact

For questions or issues, please contact the development team.
