# Disaster Resilience Hub - AI Prediction Service

AI-powered disaster risk prediction and evacuation planning service built with FastAPI and PyTorch.

## Features

- Real-time disaster risk assessment using machine learning
- Multi-factor analysis (environmental, community, health data)
- Evacuation route recommendations
- System resource monitoring and auto-scaling
- Support for multiple disaster types (earthquake, flood, wildfire, hurricane, tornado, tsunami)
- RESTful API with OpenAPI documentation

## Technology Stack

- **Framework**: FastAPI
- **ML Framework**: PyTorch
- **Data Processing**: NumPy, Pandas, GeoPandas
- **Server**: Uvicorn/Gunicorn
- **Testing**: Pytest

## Installation

### Prerequisites

- Python 3.11+
- pip
- (Optional) CUDA-compatible GPU for faster predictions

### Setup

1. Install dependencies:
```bash
pip install -r requirements.txt
```

2. Configure environment variables:
```bash
cp .env.example .env
# Edit .env with your configuration
```

3. (Optional) Train a model:
```bash
python model_trainer.py
```

4. Run the service:
```bash
python disaster_predictor.py
```

The service will be available at `http://localhost:8000`

## Docker Deployment

### Build Docker Image

```bash
# Development build
docker build --target development -t disaster-predictor:dev .

# Production build
docker build --target production -t disaster-predictor:prod .
```

### Run Docker Container

```bash
# Development
docker run -p 8000:8000 -v $(pwd)/models:/app/models disaster-predictor:dev

# Production
docker run -p 8000:8000 -e WORKERS=4 disaster-predictor:prod
```

## API Endpoints

### Health Check
```
GET /health
```

Returns service health status and model information.

### System Resources
```
GET /resources/check
```

Returns system resource usage and recommended worker count.

### Predict Risk
```
POST /predict/risk
```

Predicts disaster risk based on input data.

**Request Body:**
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
    "seismic_activity": 6.5
  },
  "community_data": {
    "population_density": 7200,
    "infrastructure_quality": 7,
    "preparedness_level": 6
  }
}
```

**Response:**
```json
{
  "risk_level": "High",
  "risk_score": 0.75,
  "confidence": 0.82,
  "urgency": "high",
  "estimated_time_to_impact_hours": 12.5,
  "recommendations": [
    "Activate emergency response protocols immediately",
    "Alert all residents through emergency notification system"
  ]
}
```

### Generate Evacuation Plan
```
POST /predict/evacuation
```

Generates evacuation plan with routes and resource requirements.

**Request Body:**
```json
{
  "location": {
    "latitude": 37.7749,
    "longitude": -122.4194
  },
  "disaster_type": "wildfire",
  "population_at_risk": 5000,
  "environmental_data": {
    "temperature": 35,
    "humidity": 20,
    "wind_speed": 45,
    "fire_danger_index": 8.5
  }
}
```

**Response:**
```json
{
  "evacuation_priority": "IMMEDIATE",
  "routes": [
    {
      "route_id": "primary",
      "distance_km": 45.2,
      "estimated_time_minutes": 45
    }
  ],
  "shelter_locations": [
    {
      "id": "shelter_1",
      "name": "Community Center Shelter",
      "capacity": 1000
    }
  ],
  "capacity_requirements": {
    "total_people": 5000,
    "transport_vehicles_needed": 100,
    "shelter_capacity_needed": 6000
  }
}
```

## API Documentation

Once the service is running, visit:
- Swagger UI: `http://localhost:8000/docs`
- ReDoc: `http://localhost:8000/redoc`

## Model Training

Train a custom model on your disaster data:

```bash
python model_trainer.py
```

The trainer supports:
- Synthetic data generation for testing
- CSV data loading for real datasets
- Automatic hyperparameter tuning
- Training history logging
- Model checkpointing

### Custom Training Data

Prepare a CSV file with the following structure:
- Columns 1-20: Feature values (normalized 0-1)
- Column 21: Risk label (0-4: Very Low, Low, Moderate, High, Critical)

Modify `model_trainer.py` to load your data:
```python
train_loader, val_loader = trainer.load_data_from_csv(
    csv_path="path/to/your/data.csv"
)
```

## Testing

Run the test suite:

```bash
# Run all tests
pytest test_predictor.py -v

# Run with coverage
pytest test_predictor.py --cov=. --cov-report=html

# Run specific test class
pytest test_predictor.py::TestAPIEndpoints -v
```

## Performance Optimization

### Multi-threading Support

The service automatically detects available CPU cores and recommends optimal worker count:

```bash
# Auto-detect workers
WORKERS=-1 python disaster_predictor.py

# Manual worker count
WORKERS=4 gunicorn disaster_predictor:app \
  --worker-class uvicorn.workers.UvicornWorker \
  --bind 0.0.0.0:8000
```

### GPU Support

Enable GPU acceleration for faster predictions:

```bash
# Set environment variable
export TORCH_DEVICE=cuda

# Or in .env file
TORCH_DEVICE=cuda
```

## Architecture

```
ai-model/
├── disaster_predictor.py   # FastAPI application
├── predictor.py            # Prediction logic
├── model_trainer.py        # Training script
├── test_predictor.py       # Tests
├── requirements.txt        # Dependencies
├── Dockerfile             # Container build
├── .env.example           # Config template
└── models/                # Trained models
```

## Integration

### Backend Integration

The AI service is designed to integrate with the main backend:

```python
import httpx

async def get_risk_prediction(data):
    async with httpx.AsyncClient() as client:
        response = await client.post(
            "http://ai-model:8000/predict/risk",
            json=data
        )
        return response.json()
```

### Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ai-predictor
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: ai-predictor
        image: disaster-predictor:prod
        env:
        - name: WORKERS
          value: "4"
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
```

## Monitoring

The service exposes metrics for monitoring:
- `/health` - Health check endpoint
- `/resources/check` - System resource metrics

Integrate with Prometheus for comprehensive monitoring.

## Contributing

1. Follow PEP 8 style guide
2. Add tests for new features
3. Update documentation
4. Ensure all tests pass

## License

See LICENSE file in the root directory.

## Support

For issues and questions, please refer to the main project repository.
