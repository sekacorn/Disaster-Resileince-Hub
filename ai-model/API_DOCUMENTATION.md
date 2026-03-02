# Disaster Resilience Hub - API Documentation

Complete API reference for the AI Prediction Service.

## Base URL

```
http://localhost:8000
```

## Authentication

Currently, the API does not require authentication. In production, implement appropriate authentication mechanisms.

---

## Endpoints

### 1. Root Endpoint

**GET /**

Returns basic service information and available endpoints.

**Response Example:**
```json
{
  "service": "Disaster Resilience Hub - AI Prediction Service",
  "version": "1.0.0",
  "status": "running",
  "endpoints": {
    "health": "/health",
    "resources": "/resources/check",
    "predict_risk": "/predict/risk",
    "predict_evacuation": "/predict/evacuation"
  }
}
```

---

### 2. Health Check

**GET /health**

Check the health status of the service and model.

**Response Schema:**
```json
{
  "status": "healthy|degraded",
  "timestamp": "2025-10-20T12:00:00",
  "model_loaded": true,
  "device": "cuda|cpu"
}
```

**Status Codes:**
- `200`: Service is operational
- `503`: Service unavailable

---

### 3. System Resources Check

**GET /resources/check**

Get current system resource usage and recommendations.

**Response Schema:**
```json
{
  "cpu_count": 8,
  "cpu_percent": 45.2,
  "memory_total_gb": 16.0,
  "memory_available_gb": 8.5,
  "memory_percent": 46.9,
  "disk_usage_percent": 62.3,
  "system_load": [2.1, 2.3, 2.2],
  "recommended_workers": 4,
  "timestamp": "2025-10-20T12:00:00"
}
```

**Status Codes:**
- `200`: Success
- `500`: Resource check failed

---

### 4. Predict Disaster Risk

**POST /predict/risk**

Predict disaster risk based on environmental, community, and health data.

**Request Schema:**

```json
{
  "location": {
    "latitude": 37.7749,
    "longitude": -122.4194,
    "elevation": 16,
    "coastal_proximity": 5
  },
  "disaster_type": "earthquake",
  "environmental_data": {
    "temperature": 18,
    "humidity": 65,
    "wind_speed": 15,
    "precipitation": 10,
    "seismic_activity": 6.5,
    "flood_risk": 2.0,
    "fire_danger_index": 3.0
  },
  "community_data": {
    "population_density": 7200,
    "infrastructure_quality": 7,
    "preparedness_level": 6,
    "vulnerable_population_ratio": 0.15,
    "emergency_services_capacity": 8
  },
  "health_data": {
    "hospital_capacity": 0.7,
    "medical_supply_level": 0.8,
    "disease_outbreak_risk": 1.0
  },
  "historical_disaster_count": 12
}
```

**Field Descriptions:**

| Field | Type | Required | Range | Description |
|-------|------|----------|-------|-------------|
| `location.latitude` | float | Yes | -90 to 90 | Latitude coordinate |
| `location.longitude` | float | Yes | -180 to 180 | Longitude coordinate |
| `location.elevation` | float | No | Any | Elevation in meters |
| `location.coastal_proximity` | float | No | 0+ | Distance to coast in km |
| `disaster_type` | string | Yes | - | Type of disaster (see supported types) |
| `environmental_data.temperature` | float | No | Any | Temperature in Celsius |
| `environmental_data.humidity` | float | No | 0-100 | Humidity percentage |
| `environmental_data.wind_speed` | float | No | 0+ | Wind speed in km/h |
| `environmental_data.precipitation` | float | No | 0+ | Precipitation in mm |
| `environmental_data.seismic_activity` | float | No | 0-10 | Seismic activity level |
| `environmental_data.flood_risk` | float | No | 0-10 | Flood risk level |
| `environmental_data.fire_danger_index` | float | No | 0-10 | Fire danger index |
| `community_data.population_density` | float | No | 0+ | People per sq km |
| `community_data.infrastructure_quality` | float | No | 0-10 | Infrastructure quality score |
| `community_data.preparedness_level` | float | No | 0-10 | Preparedness level score |
| `community_data.vulnerable_population_ratio` | float | No | 0-1 | Ratio of vulnerable population |
| `community_data.emergency_services_capacity` | float | No | 0-10 | Emergency services capacity |
| `health_data.hospital_capacity` | float | No | 0-1 | Hospital capacity utilization |
| `health_data.medical_supply_level` | float | No | 0-1 | Medical supply level |
| `health_data.disease_outbreak_risk` | float | No | 0-10 | Disease outbreak risk |
| `historical_disaster_count` | integer | No | 0+ | Historical disaster count |

**Supported Disaster Types:**
- `earthquake`
- `flood`
- `wildfire`
- `hurricane`
- `tornado`
- `tsunami`
- `landslide`
- `drought`

**Response Schema:**

```json
{
  "risk_level": "High",
  "risk_score": 0.75,
  "confidence": 0.82,
  "risk_breakdown": {
    "Very Low": 0.05,
    "Low": 0.08,
    "Moderate": 0.15,
    "High": 0.42,
    "Critical": 0.30
  },
  "disaster_type": "earthquake",
  "urgency": "high",
  "estimated_time_to_impact_hours": 12.5,
  "recommendations": [
    "Activate emergency response protocols immediately",
    "Consider immediate evacuation of high-risk areas",
    "Alert all residents through emergency notification system",
    "Deploy emergency services to strategic locations",
    "Ensure emergency shelters are prepared and staffed",
    "Secure heavy furniture and objects",
    "Identify safe spots in buildings",
    "Check structural integrity of buildings"
  ],
  "timestamp": "2025-10-20T12:00:00"
}
```

**Risk Levels:**
- `Very Low`: Risk score < 0.2
- `Low`: Risk score 0.2 - 0.4
- `Moderate`: Risk score 0.4 - 0.6
- `High`: Risk score 0.6 - 0.8
- `Critical`: Risk score > 0.8

**Urgency Levels:**
- `low`: Standard monitoring
- `medium`: Enhanced monitoring
- `high`: Immediate attention required
- `critical`: Emergency response required

**Status Codes:**
- `200`: Prediction successful
- `400`: Invalid request data
- `422`: Validation error
- `500`: Prediction failed
- `503`: Service unavailable

---

### 5. Generate Evacuation Plan

**POST /predict/evacuation**

Generate comprehensive evacuation plan with routes, shelters, and resource requirements.

**Request Schema:**

```json
{
  "location": {
    "latitude": 34.0522,
    "longitude": -118.2437,
    "elevation": 71
  },
  "destination": {
    "latitude": 34.1522,
    "longitude": -118.1437,
    "elevation": 100
  },
  "disaster_type": "wildfire",
  "population_at_risk": 5000,
  "environmental_data": {
    "temperature": 35,
    "humidity": 20,
    "wind_speed": 45,
    "fire_danger_index": 8.5
  },
  "community_data": {
    "population_density": 3000,
    "vulnerable_population_ratio": 0.25,
    "preparedness_level": 7
  },
  "health_data": {
    "hospital_capacity": 0.6,
    "medical_supply_level": 0.75
  },
  "historical_disaster_count": 8
}
```

**Field Descriptions:**

All fields from risk prediction, plus:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `destination.latitude` | float | No | Destination latitude |
| `destination.longitude` | float | No | Destination longitude |
| `destination.elevation` | float | No | Destination elevation |
| `population_at_risk` | integer | Yes | Number of people to evacuate |

**Response Schema:**

```json
{
  "evacuation_priority": "HIGH",
  "risk_assessment": {
    "risk_level": "High",
    "risk_score": 0.75,
    "confidence": 0.82,
    "urgency": "high",
    "estimated_time_to_impact_hours": 6.0,
    "recommendations": [...]
  },
  "routes": [
    {
      "route_id": "primary",
      "name": "Primary Evacuation Route",
      "start": {
        "latitude": 34.0522,
        "longitude": -118.2437
      },
      "end": {
        "latitude": 34.1522,
        "longitude": -118.1437
      },
      "distance_km": 15.2,
      "estimated_time_minutes": 45,
      "capacity": "high",
      "road_conditions": "good",
      "waypoints": []
    },
    {
      "route_id": "alternate_1",
      "name": "Alternate Route 1",
      "start": {...},
      "end": {...},
      "distance_km": 18.5,
      "estimated_time_minutes": 60,
      "capacity": "medium",
      "road_conditions": "fair",
      "waypoints": []
    }
  ],
  "capacity_requirements": {
    "total_people": 5000,
    "vulnerable_individuals": 1250,
    "transport_vehicles_needed": 100,
    "shelter_capacity_needed": 6000,
    "medical_personnel_needed": 25,
    "supplies_needed": {
      "water_liters": 15000,
      "food_meals": 15000,
      "blankets": 2500,
      "medical_kits": 50
    }
  },
  "estimated_evacuation_time_hours": 4.5,
  "assembly_points": [
    {
      "id": "ap_1",
      "name": "Primary Assembly Point",
      "location": {
        "latitude": 34.0622,
        "longitude": -118.2337
      },
      "capacity": 500,
      "facilities": ["parking", "restrooms", "shelter"]
    }
  ],
  "shelter_locations": [
    {
      "id": "shelter_1",
      "name": "Community Center Shelter",
      "location": {
        "latitude": 34.1022,
        "longitude": -118.1937
      },
      "capacity": 1000,
      "facilities": ["food", "water", "medical", "power", "communications"],
      "accessibility": "wheelchair_accessible"
    }
  ],
  "special_needs_considerations": {
    "mobility_assistance_needed": true,
    "medical_transport_required": true,
    "language_support": ["English", "Spanish", "Mandarin"],
    "pet_accommodation": true,
    "medical_equipment_power": true,
    "dietary_restrictions": true
  },
  "timestamp": "2025-10-20T12:00:00"
}
```

**Evacuation Priority Levels:**
- `LOW`: Standard evacuation timeline
- `MEDIUM`: Accelerated evacuation
- `HIGH`: Urgent evacuation required
- `IMMEDIATE`: Emergency evacuation now

**Status Codes:**
- `200`: Evacuation plan generated
- `400`: Invalid request data
- `422`: Validation error
- `500`: Planning failed
- `503`: Service unavailable

---

## Error Responses

All error responses follow this format:

```json
{
  "detail": "Error message describing what went wrong"
}
```

### Common Error Codes

| Code | Description |
|------|-------------|
| 400 | Bad Request - Invalid input data |
| 422 | Validation Error - Request data failed validation |
| 500 | Internal Server Error - Server-side error occurred |
| 503 | Service Unavailable - Service is not ready |

---

## Rate Limiting

Currently, no rate limiting is implemented. In production, consider implementing rate limiting to prevent abuse.

---

## Interactive Documentation

The API provides interactive documentation:

- **Swagger UI**: `http://localhost:8000/docs`
- **ReDoc**: `http://localhost:8000/redoc`

---

## Example Requests

### Using cURL

**Risk Prediction:**
```bash
curl -X POST http://localhost:8000/predict/risk \
  -H "Content-Type: application/json" \
  -d '{
    "location": {"latitude": 37.7749, "longitude": -122.4194},
    "disaster_type": "earthquake",
    "environmental_data": {"seismic_activity": 6.5}
  }'
```

**Evacuation Plan:**
```bash
curl -X POST http://localhost:8000/predict/evacuation \
  -H "Content-Type: application/json" \
  -d '{
    "location": {"latitude": 34.0522, "longitude": -118.2437},
    "disaster_type": "wildfire",
    "population_at_risk": 5000
  }'
```

### Using Python

```python
import httpx

async def predict_risk():
    async with httpx.AsyncClient() as client:
        response = await client.post(
            "http://localhost:8000/predict/risk",
            json={
                "location": {"latitude": 37.7749, "longitude": -122.4194},
                "disaster_type": "earthquake",
                "environmental_data": {"seismic_activity": 6.5}
            }
        )
        return response.json()
```

### Using JavaScript

```javascript
async function predictRisk() {
  const response = await fetch('http://localhost:8000/predict/risk', {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify({
      location: {latitude: 37.7749, longitude: -122.4194},
      disaster_type: 'earthquake',
      environmental_data: {seismic_activity: 6.5}
    })
  });
  return await response.json();
}
```

---

## Response Times

Typical response times (on standard hardware):

- Health Check: < 50ms
- Resource Check: < 100ms
- Risk Prediction: 100-500ms
- Evacuation Plan: 200-800ms

Response times may vary based on system load and hardware.

---

## Best Practices

1. **Include All Available Data**: More data leads to better predictions
2. **Cache Results**: Cache predictions for identical inputs
3. **Handle Errors Gracefully**: Always check response status codes
4. **Use Async Requests**: For better performance in applications
5. **Monitor System Resources**: Check `/resources/check` before heavy loads
6. **Validate Input Data**: Validate data before sending to API
7. **Update Regularly**: Re-query for updated predictions as conditions change

---

## Support

For issues, questions, or feature requests, please refer to the main project repository or contact the development team.

---

## Version History

- **v1.0.0** (2025-10-20): Initial release
  - Risk prediction endpoint
  - Evacuation planning endpoint
  - Health and resource monitoring
  - Multi-disaster type support
