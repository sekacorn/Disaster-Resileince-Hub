"""
FastAPI Application for Disaster Risk Prediction Service
Provides REST API endpoints for disaster prediction and evacuation planning
"""

from fastapi import FastAPI, HTTPException, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import Dict, List, Optional
import logging
import psutil
import os
from datetime import datetime
from contextlib import asynccontextmanager

from predictor import DisasterPredictor

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


# Pydantic models for request/response validation
class LocationData(BaseModel):
    latitude: float = Field(..., ge=-90, le=90, description="Latitude coordinate")
    longitude: float = Field(..., ge=-180, le=180, description="Longitude coordinate")
    elevation: Optional[float] = Field(0, description="Elevation in meters")
    coastal_proximity: Optional[float] = Field(0, description="Distance to coast in km")


class EnvironmentalData(BaseModel):
    temperature: Optional[float] = Field(20, description="Temperature in Celsius")
    humidity: Optional[float] = Field(50, ge=0, le=100, description="Humidity percentage")
    wind_speed: Optional[float] = Field(0, ge=0, description="Wind speed in km/h")
    precipitation: Optional[float] = Field(0, ge=0, description="Precipitation in mm")
    seismic_activity: Optional[float] = Field(0, ge=0, le=10, description="Seismic activity level")
    flood_risk: Optional[float] = Field(0, ge=0, le=10, description="Flood risk level")
    fire_danger_index: Optional[float] = Field(0, ge=0, le=10, description="Fire danger index")


class CommunityData(BaseModel):
    population_density: Optional[float] = Field(100, ge=0, description="People per sq km")
    infrastructure_quality: Optional[float] = Field(5, ge=0, le=10, description="Infrastructure quality score")
    preparedness_level: Optional[float] = Field(5, ge=0, le=10, description="Preparedness level score")
    vulnerable_population_ratio: Optional[float] = Field(0.2, ge=0, le=1, description="Vulnerable population ratio")
    emergency_services_capacity: Optional[float] = Field(5, ge=0, le=10, description="Emergency services capacity")


class HealthData(BaseModel):
    hospital_capacity: Optional[float] = Field(0.5, ge=0, le=1, description="Hospital capacity utilization")
    medical_supply_level: Optional[float] = Field(0.7, ge=0, le=1, description="Medical supply level")
    disease_outbreak_risk: Optional[float] = Field(0, ge=0, le=10, description="Disease outbreak risk")


class RiskPredictionRequest(BaseModel):
    location: LocationData
    disaster_type: str = Field(..., description="Type of disaster (earthquake, flood, wildfire, hurricane, etc.)")
    environmental_data: Optional[EnvironmentalData] = None
    community_data: Optional[CommunityData] = None
    health_data: Optional[HealthData] = None
    historical_disaster_count: Optional[int] = Field(0, ge=0, description="Historical disaster count")

    class Config:
        json_schema_extra = {
            "example": {
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
                },
                "historical_disaster_count": 12
            }
        }


class EvacuationRequest(BaseModel):
    location: LocationData
    destination: Optional[LocationData] = None
    disaster_type: str = Field(..., description="Type of disaster")
    environmental_data: Optional[EnvironmentalData] = None
    community_data: Optional[CommunityData] = None
    health_data: Optional[HealthData] = None
    population_at_risk: Optional[int] = Field(1000, ge=0, description="Number of people at risk")
    historical_disaster_count: Optional[int] = Field(0, ge=0)

    class Config:
        json_schema_extra = {
            "example": {
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
        }


class RiskPredictionResponse(BaseModel):
    risk_level: str
    risk_score: float
    confidence: float
    risk_breakdown: Dict[str, float]
    disaster_type: str
    urgency: str
    estimated_time_to_impact_hours: float
    recommendations: List[str]
    timestamp: str


class EvacuationResponse(BaseModel):
    evacuation_priority: str
    risk_assessment: RiskPredictionResponse
    routes: List[Dict]
    capacity_requirements: Dict
    estimated_evacuation_time_hours: float
    assembly_points: List[Dict]
    shelter_locations: List[Dict]
    special_needs_considerations: Dict
    timestamp: str


class HealthResponse(BaseModel):
    status: str
    timestamp: str
    model_loaded: bool
    device: str


class ResourcesResponse(BaseModel):
    cpu_count: int
    cpu_percent: float
    memory_total_gb: float
    memory_available_gb: float
    memory_percent: float
    disk_usage_percent: float
    system_load: List[float]
    recommended_workers: int
    timestamp: str


# Global predictor instance
predictor: Optional[DisasterPredictor] = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Startup and shutdown events"""
    global predictor

    # Startup
    logger.info("Initializing Disaster Prediction Service...")

    # Load model if exists
    model_path = os.environ.get("MODEL_PATH", "models/disaster_risk_model.pth")

    try:
        predictor = DisasterPredictor(model_path=model_path if os.path.exists(model_path) else None)
        logger.info("Predictor initialized successfully")
    except Exception as e:
        logger.warning(f"Could not load model: {e}")
        predictor = DisasterPredictor()
        logger.info("Using untrained predictor")

    yield

    # Shutdown
    logger.info("Shutting down Disaster Prediction Service...")


# Create FastAPI app
app = FastAPI(
    title="Disaster Resilience Hub - AI Prediction Service",
    description="AI-powered disaster risk prediction and evacuation planning service",
    version="1.0.0",
    lifespan=lifespan
)


# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Configure appropriately for production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# Endpoints
@app.get("/", tags=["Root"])
async def root():
    """Root endpoint"""
    return {
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


@app.get("/health", response_model=HealthResponse, tags=["Health"])
async def health_check():
    """Health check endpoint"""

    try:
        import torch
        device = str(predictor.device if predictor else "unknown")
        model_loaded = predictor is not None and predictor.model is not None
    except Exception as e:
        logger.error(f"Health check error: {e}")
        device = "unknown"
        model_loaded = False

    return HealthResponse(
        status="healthy" if model_loaded else "degraded",
        timestamp=datetime.utcnow().isoformat(),
        model_loaded=model_loaded,
        device=device
    )


@app.get("/resources/check", response_model=ResourcesResponse, tags=["System"])
async def check_resources():
    """Check system resources and provide recommendations"""

    try:
        cpu_count = psutil.cpu_count()
        cpu_percent = psutil.cpu_percent(interval=1)

        memory = psutil.virtual_memory()
        memory_total_gb = memory.total / (1024 ** 3)
        memory_available_gb = memory.available / (1024 ** 3)
        memory_percent = memory.percent

        disk = psutil.disk_usage('/')
        disk_usage_percent = disk.percent

        # Get system load average (Unix-like systems)
        try:
            system_load = list(os.getloadavg())
        except (AttributeError, OSError):
            # Windows doesn't have getloadavg, use CPU percent as approximation
            system_load = [cpu_percent / 100.0 * cpu_count] * 3

        # Calculate recommended workers based on resources
        recommended_workers = max(1, min(cpu_count, int(memory_available_gb)))

        return ResourcesResponse(
            cpu_count=cpu_count,
            cpu_percent=cpu_percent,
            memory_total_gb=round(memory_total_gb, 2),
            memory_available_gb=round(memory_available_gb, 2),
            memory_percent=memory_percent,
            disk_usage_percent=disk_usage_percent,
            system_load=[round(load, 2) for load in system_load],
            recommended_workers=recommended_workers,
            timestamp=datetime.utcnow().isoformat()
        )

    except Exception as e:
        logger.error(f"Resource check error: {e}")
        raise HTTPException(status_code=500, detail=f"Resource check failed: {str(e)}")


@app.post("/predict/risk", response_model=RiskPredictionResponse, tags=["Prediction"])
async def predict_risk(request: RiskPredictionRequest, background_tasks: BackgroundTasks):
    """
    Predict disaster risk based on environmental, community, and health data.

    Returns risk level, confidence score, and recommendations.
    """

    if predictor is None:
        raise HTTPException(status_code=503, detail="Predictor not initialized")

    try:
        logger.info(f"Risk prediction request for {request.disaster_type} at ({request.location.latitude}, {request.location.longitude})")

        # Convert request to dictionary format
        data = {
            "location": request.location.dict(),
            "disaster_type": request.disaster_type,
            "environmental_data": request.environmental_data.dict() if request.environmental_data else {},
            "community_data": request.community_data.dict() if request.community_data else {},
            "health_data": request.health_data.dict() if request.health_data else {},
            "historical_disaster_count": request.historical_disaster_count
        }

        # Make prediction
        result = predictor.predict_risk(data)

        # Log result in background
        background_tasks.add_task(
            log_prediction,
            request.disaster_type,
            result["risk_level"],
            result["risk_score"]
        )

        return RiskPredictionResponse(**result)

    except Exception as e:
        logger.error(f"Risk prediction error: {e}")
        raise HTTPException(status_code=500, detail=f"Prediction failed: {str(e)}")


@app.post("/predict/evacuation", response_model=EvacuationResponse, tags=["Prediction"])
async def predict_evacuation(request: EvacuationRequest, background_tasks: BackgroundTasks):
    """
    Generate evacuation plan with route recommendations.

    Returns evacuation priority, routes, shelter locations, and capacity requirements.
    """

    if predictor is None:
        raise HTTPException(status_code=503, detail="Predictor not initialized")

    try:
        logger.info(f"Evacuation plan request for {request.population_at_risk} people at ({request.location.latitude}, {request.location.longitude})")

        # Convert request to dictionary format
        data = {
            "location": request.location.dict(),
            "destination": request.destination.dict() if request.destination else {},
            "disaster_type": request.disaster_type,
            "environmental_data": request.environmental_data.dict() if request.environmental_data else {},
            "community_data": request.community_data.dict() if request.community_data else {},
            "health_data": request.health_data.dict() if request.health_data else {},
            "population_at_risk": request.population_at_risk,
            "historical_disaster_count": request.historical_disaster_count
        }

        # Generate evacuation plan
        result = predictor.generate_evacuation_plan(data)

        # Log evacuation plan in background
        background_tasks.add_task(
            log_evacuation_plan,
            request.disaster_type,
            result["evacuation_priority"],
            request.population_at_risk
        )

        # Convert nested risk_assessment to response model
        risk_assessment_data = result["risk_assessment"]
        risk_assessment = RiskPredictionResponse(**risk_assessment_data)
        result["risk_assessment"] = risk_assessment

        return EvacuationResponse(**result)

    except Exception as e:
        logger.error(f"Evacuation planning error: {e}")
        raise HTTPException(status_code=500, detail=f"Evacuation planning failed: {str(e)}")


# Background task functions
def log_prediction(disaster_type: str, risk_level: str, risk_score: float):
    """Log prediction for analytics"""
    logger.info(f"PREDICTION_LOG: {disaster_type} | {risk_level} | {risk_score:.3f}")


def log_evacuation_plan(disaster_type: str, priority: str, population: int):
    """Log evacuation plan for analytics"""
    logger.info(f"EVACUATION_LOG: {disaster_type} | {priority} | Population: {population}")


# Error handlers
@app.exception_handler(ValueError)
async def value_error_handler(request, exc):
    return HTTPException(status_code=400, detail=str(exc))


@app.exception_handler(Exception)
async def general_exception_handler(request, exc):
    logger.error(f"Unhandled exception: {exc}")
    return HTTPException(status_code=500, detail="Internal server error")


if __name__ == "__main__":
    import uvicorn

    # Get configuration from environment
    host = os.environ.get("HOST", "0.0.0.0")
    port = int(os.environ.get("PORT", 8000))
    workers = int(os.environ.get("WORKERS", 1))

    # Determine number of workers based on CPU
    if workers == -1:
        workers = max(1, psutil.cpu_count() - 1)

    logger.info(f"Starting server on {host}:{port} with {workers} workers")

    uvicorn.run(
        "disaster_predictor:app",
        host=host,
        port=port,
        workers=workers,
        reload=False,
        log_level="info"
    )
