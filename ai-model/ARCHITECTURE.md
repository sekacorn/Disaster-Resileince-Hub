# AI Prediction Service - Architecture

## System Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Client Applications                           │
│  (Web Browser, Mobile App, Backend Services, Command Line)          │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             │ HTTP/REST API
                             │
┌────────────────────────────▼────────────────────────────────────────┐
│                      FastAPI Application                             │
│                    (disaster_predictor.py)                           │
│                                                                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐             │
│  │   /health    │  │ /predict/    │  │ /predict/    │             │
│  │              │  │   risk       │  │ evacuation   │             │
│  └──────────────┘  └──────────────┘  └──────────────┘             │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────┐     │
│  │          Request Validation (Pydantic)                    │     │
│  │  ├─ LocationData                                          │     │
│  │  ├─ EnvironmentalData                                     │     │
│  │  ├─ CommunityData                                         │     │
│  │  └─ HealthData                                            │     │
│  └──────────────────────────────────────────────────────────┘     │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             │
┌────────────────────────────▼────────────────────────────────────────┐
│                    Prediction Engine                                 │
│                      (predictor.py)                                  │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────┐     │
│  │         DisasterPredictor Class                           │     │
│  │  ├─ preprocess_features()                                 │     │
│  │  ├─ predict_risk()                                        │     │
│  │  ├─ generate_evacuation_plan()                            │     │
│  │  ├─ calculate_distance()                                  │     │
│  │  └─ generate_recommendations()                            │     │
│  └────────────────────┬─────────────────────────────────────┘     │
│                       │                                             │
│  ┌────────────────────▼─────────────────────────────────────┐     │
│  │         PyTorch Neural Network                            │     │
│  │         (DisasterRiskModel)                               │     │
│  │                                                            │     │
│  │  Input (20 features)                                      │     │
│  │    ↓                                                       │     │
│  │  Hidden Layer 1 (128) + ReLU + BatchNorm + Dropout       │     │
│  │    ↓                                                       │     │
│  │  Hidden Layer 2 (64) + ReLU + BatchNorm + Dropout        │     │
│  │    ↓                                                       │     │
│  │  Hidden Layer 3 (32) + ReLU + BatchNorm + Dropout        │     │
│  │    ↓                                                       │     │
│  │  Output (5 risk categories) + Softmax                    │     │
│  └──────────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────────────┘
                             │
                             │
┌────────────────────────────▼────────────────────────────────────────┐
│                    Configuration Layer                               │
│                       (config.py)                                    │
│                                                                      │
│  ├─ Settings Management                                             │
│  ├─ Disaster Type Definitions                                       │
│  ├─ Risk Thresholds                                                 │
│  ├─ Feature Ranges                                                  │
│  └─ Resource Multipliers                                            │
└─────────────────────────────────────────────────────────────────────┘
```

## Data Flow

### 1. Risk Prediction Flow

```
Client Request
    │
    ├─> Location Data (lat, lon, elevation)
    ├─> Disaster Type (earthquake, flood, etc.)
    ├─> Environmental Data (temp, humidity, wind, etc.)
    ├─> Community Data (population, infrastructure, etc.)
    └─> Health Data (hospital capacity, supplies, etc.)
    │
    ▼
FastAPI Endpoint (/predict/risk)
    │
    ├─> Request Validation (Pydantic)
    └─> Forward to Predictor
    │
    ▼
Feature Preprocessing
    │
    ├─> Normalize values (0-1 range)
    ├─> Create 20-feature tensor
    └─> Handle missing values
    │
    ▼
Neural Network Inference
    │
    ├─> Forward pass through network
    ├─> Get probability distribution
    └─> Extract confidence scores
    │
    ▼
Risk Assessment
    │
    ├─> Determine risk level (Very Low to Critical)
    ├─> Calculate overall risk score
    ├─> Estimate time to impact
    └─> Generate recommendations
    │
    ▼
Response to Client
    │
    └─> JSON with risk analysis and recommendations
```

### 2. Evacuation Planning Flow

```
Client Request
    │
    ├─> Origin Location
    ├─> Destination (optional)
    ├─> Population at Risk
    └─> Disaster Context
    │
    ▼
Risk Assessment (same as above)
    │
    ▼
Evacuation Planning
    │
    ├─> Calculate Evacuation Priority
    │   └─> Based on risk score + population
    │
    ├─> Generate Routes
    │   ├─> Primary route
    │   ├─> Alternate routes
    │   └─> Emergency route
    │
    ├─> Calculate Resource Requirements
    │   ├─> Transport vehicles
    │   ├─> Medical personnel
    │   ├─> Shelter capacity
    │   └─> Supplies (water, food, medical)
    │
    ├─> Identify Assembly Points
    │   └─> Safe gathering locations
    │
    └─> Locate Shelters
        └─> Emergency shelter facilities
    │
    ▼
Response to Client
    │
    └─> JSON with complete evacuation plan
```

## Component Details

### 1. FastAPI Application Layer

**File**: `disaster_predictor.py`

**Responsibilities**:
- HTTP request handling
- Input validation with Pydantic
- Response serialization
- Error handling
- Background task management
- CORS configuration
- Health monitoring
- Resource tracking

**Key Components**:
- Pydantic request/response models
- Route handlers (@app.post, @app.get)
- Middleware (CORS)
- Lifespan management
- Exception handlers

### 2. Prediction Engine

**File**: `predictor.py`

**Responsibilities**:
- Feature engineering
- Model inference
- Risk calculation
- Recommendation generation
- Distance calculations
- Route planning
- Resource estimation

**Key Classes**:
- `DisasterRiskModel`: Neural network model
- `DisasterPredictor`: Main prediction logic

**Key Methods**:
- `preprocess_features()`: Normalize and prepare data
- `predict_risk()`: Risk assessment
- `generate_evacuation_plan()`: Evacuation planning
- `_calculate_distance()`: Haversine formula
- `_generate_recommendations()`: Context-aware advice

### 3. Model Training Pipeline

**File**: `model_trainer.py`

**Responsibilities**:
- Data loading and preprocessing
- Model training
- Validation
- Checkpointing
- Metrics tracking

**Key Classes**:
- `DisasterDataset`: PyTorch dataset
- `ModelTrainer`: Training pipeline

**Features**:
- Synthetic data generation
- CSV data loading
- Early stopping
- Learning rate scheduling
- Training history logging

### 4. Configuration Management

**File**: `config.py`

**Responsibilities**:
- Environment variable management
- Disaster type definitions
- Risk thresholds
- Feature ranges
- Resource multipliers

**Key Classes**:
- `Settings`: Application configuration

### 5. Testing Suite

**File**: `test_predictor.py`

**Test Categories**:
- API endpoint tests
- Predictor logic tests
- Model tests
- Integration tests
- Performance tests

## Deployment Architecture

### Docker Deployment

```
┌─────────────────────────────────────────┐
│         Docker Container                 │
│                                          │
│  ┌────────────────────────────────┐    │
│  │   Gunicorn (Master Process)    │    │
│  │                                 │    │
│  │  ┌──────────┐  ┌──────────┐   │    │
│  │  │ Worker 1 │  │ Worker 2 │   │    │
│  │  │ Uvicorn  │  │ Uvicorn  │   │    │
│  │  └──────────┘  └──────────┘   │    │
│  │                                 │    │
│  │  ┌──────────┐  ┌──────────┐   │    │
│  │  │ Worker 3 │  │ Worker 4 │   │    │
│  │  │ Uvicorn  │  │ Uvicorn  │   │    │
│  │  └──────────┘  └──────────┘   │    │
│  └────────────────────────────────┘    │
│                                          │
│  Volumes:                                │
│  ├─ ./models → /app/models              │
│  └─ ./logs → /app/logs                  │
└─────────────────────────────────────────┘
```

### Kubernetes Deployment

```
┌─────────────────────────────────────────────────────────┐
│                    Load Balancer                         │
│                    (Service)                             │
└────────────────────────┬────────────────────────────────┘
                         │
         ┌───────────────┼───────────────┐
         │               │               │
┌────────▼──────┐ ┌─────▼──────┐ ┌─────▼──────┐
│   Pod 1       │ │   Pod 2    │ │   Pod 3    │
│               │ │            │ │            │
│ AI Predictor  │ │ AI Pred.   │ │ AI Pred.   │
│ Container     │ │ Container  │ │ Container  │
└───────────────┘ └────────────┘ └────────────┘
         │               │               │
         └───────────────┼───────────────┘
                         │
                ┌────────▼─────────┐
                │ Persistent Volume │
                │   (Models)        │
                └───────────────────┘
```

## Integration Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    DisasterResilienceHub                         │
│                      Backend (Node.js)                           │
│                                                                  │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐               │
│  │  User API  │  │  Alert API │  │  Map API   │               │
│  └──────┬─────┘  └──────┬─────┘  └──────┬─────┘               │
│         │               │               │                       │
│         └───────────────┼───────────────┘                       │
│                         │                                        │
│                         │ HTTP Request                          │
└─────────────────────────┼────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                  AI Prediction Service                           │
│                   (Python/FastAPI)                               │
│                                                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │ Risk API     │  │ Evacuation   │  │ Monitoring   │         │
│  │ /predict/    │  │ API          │  │ API          │         │
│  │ risk         │  │ /predict/    │  │ /health      │         │
│  │              │  │ evacuation   │  │ /resources   │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
└─────────────────────────────────────────────────────────────────┘
                          │
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                    External Services                             │
│                                                                  │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐               │
│  │  Weather   │  │  Seismic   │  │   Maps     │               │
│  │    API     │  │   Monitor  │  │   API      │               │
│  └────────────┘  └────────────┘  └────────────┘               │
└─────────────────────────────────────────────────────────────────┘
```

## Security Architecture

```
┌─────────────────────────────────────────┐
│           Client Request                 │
└───────────────┬─────────────────────────┘
                │
                ▼
┌───────────────────────────────────────────┐
│         Firewall / WAF                    │
└───────────────┬───────────────────────────┘
                │
                ▼
┌───────────────────────────────────────────┐
│      Load Balancer (HTTPS/TLS)           │
└───────────────┬───────────────────────────┘
                │
                ▼
┌───────────────────────────────────────────┐
│     API Gateway (Rate Limiting)           │
└───────────────┬───────────────────────────┘
                │
                ▼
┌───────────────────────────────────────────┐
│   Authentication/Authorization            │
│   (Future: JWT, OAuth)                    │
└───────────────┬───────────────────────────┘
                │
                ▼
┌───────────────────────────────────────────┐
│      FastAPI Application                  │
│      (Input Validation)                   │
└───────────────────────────────────────────┘
```

## Monitoring Architecture

```
┌─────────────────────────────────────────┐
│       AI Prediction Service              │
│                                          │
│  ┌────────────────────────────────┐    │
│  │   Application Metrics          │    │
│  │   ├─ Request count             │    │
│  │   ├─ Response times            │    │
│  │   ├─ Error rates               │    │
│  │   └─ Active requests           │    │
│  └─────────────┬──────────────────┘    │
│                │                         │
│  ┌─────────────▼──────────────────┐    │
│  │   System Metrics               │    │
│  │   ├─ CPU usage                 │    │
│  │   ├─ Memory usage              │    │
│  │   ├─ Disk usage                │    │
│  │   └─ Network I/O               │    │
│  └─────────────┬──────────────────┘    │
└────────────────┼───────────────────────┘
                 │
                 ▼
┌────────────────────────────────────────┐
│      Monitoring Stack                  │
│                                        │
│  ┌──────────────────────────────┐    │
│  │     Prometheus                │    │
│  │     (Metrics Collection)      │    │
│  └────────────┬─────────────────┘    │
│               │                       │
│  ┌────────────▼─────────────────┐    │
│  │     Grafana                   │    │
│  │     (Visualization)           │    │
│  └──────────────────────────────┘    │
│                                        │
│  ┌──────────────────────────────┐    │
│  │     AlertManager              │    │
│  │     (Alerting)                │    │
│  └──────────────────────────────┘    │
└────────────────────────────────────────┘
```

## Performance Optimization

### Caching Strategy

```
Request → Check Cache → Cache Hit?
                           │
                 ┌─────────┴─────────┐
                 │                   │
               Yes                  No
                 │                   │
                 ▼                   ▼
         Return Cached         Run Prediction
            Result                    │
                 │                    ▼
                 │              Cache Result
                 │                    │
                 └────────┬───────────┘
                          │
                          ▼
                   Return Response
```

### Load Balancing

```
┌──────────┐
│ Request  │
└────┬─────┘
     │
     ▼
┌────────────┐     ┌──────────┐
│   Load     │────▶│ Worker 1 │
│  Balancer  │     └──────────┘
│  (Round    │     ┌──────────┐
│   Robin)   │────▶│ Worker 2 │
└────────────┘     └──────────┘
     │             ┌──────────┐
     └────────────▶│ Worker N │
                   └──────────┘
```

## File Organization

```
ai-model/
├── Core Application
│   ├── disaster_predictor.py    (FastAPI app)
│   ├── predictor.py              (Prediction engine)
│   ├── model_trainer.py          (Training pipeline)
│   └── config.py                 (Configuration)
│
├── Testing
│   ├── test_predictor.py         (Test suite)
│   └── api_client.py             (Client library + examples)
│
├── Configuration
│   ├── requirements.txt          (Dependencies)
│   ├── .env.example              (Environment template)
│   ├── pytest.ini                (Test configuration)
│   ├── .gitignore                (Git ignore rules)
│   └── .dockerignore             (Docker ignore rules)
│
├── Deployment
│   ├── Dockerfile                (Container build)
│   ├── docker-compose.yml        (Compose config)
│   ├── start.sh                  (Linux/Mac startup)
│   ├── start.bat                 (Windows startup)
│   └── Makefile                  (Build automation)
│
├── Documentation
│   ├── README.md                 (Main documentation)
│   ├── API_DOCUMENTATION.md      (API reference)
│   ├── QUICKSTART.md             (Quick start guide)
│   ├── IMPLEMENTATION_SUMMARY.md (Implementation overview)
│   └── ARCHITECTURE.md           (This file)
│
└── Runtime Directories
    ├── models/                   (Trained models)
    ├── logs/                     (Application logs)
    └── data/                     (Training data)
```

## Technology Stack

### Core Technologies

- **Python 3.11+**: Programming language
- **FastAPI**: Web framework
- **PyTorch**: Machine learning framework
- **Uvicorn**: ASGI server
- **Gunicorn**: Process manager

### Data Processing

- **NumPy**: Numerical computing
- **Pandas**: Data manipulation
- **GeoPandas**: Geospatial data
- **Scikit-learn**: ML utilities

### Validation & Testing

- **Pydantic**: Data validation
- **Pytest**: Testing framework
- **httpx**: HTTP client

### Deployment

- **Docker**: Containerization
- **Docker Compose**: Multi-container deployment

## Conclusion

The AI Prediction Service is architected as a modular, scalable, and maintainable system with clear separation of concerns. Each component has a specific responsibility and can be independently developed, tested, and deployed. The architecture supports horizontal scaling, monitoring, and integration with external systems.
