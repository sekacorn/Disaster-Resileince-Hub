"""
Configuration Management for AI Prediction Service
"""

from pydantic_settings import BaseSettings
from typing import Optional
import os


class Settings(BaseSettings):
    """Application settings loaded from environment variables"""

    # Server Configuration
    host: str = "0.0.0.0"
    port: int = 8000
    workers: int = 1
    reload: bool = False

    # Model Configuration
    model_path: str = "models/disaster_risk_model.pth"
    input_size: int = 20
    hidden_sizes: list = [128, 64, 32]

    # PyTorch Configuration
    torch_device: Optional[str] = None  # None for auto-detect
    use_cuda: bool = True  # Try to use CUDA if available

    # Training Configuration
    learning_rate: float = 0.001
    batch_size: int = 32
    num_epochs: int = 100
    early_stopping_patience: int = 20

    # Logging Configuration
    log_level: str = "INFO"
    log_format: str = "%(asctime)s - %(name)s - %(levelname)s - %(message)s"

    # CORS Configuration
    allowed_origins: str = "*"  # Comma-separated list or "*"
    allow_credentials: bool = True
    allowed_methods: str = "*"
    allowed_headers: str = "*"

    # API Configuration
    api_title: str = "Disaster Resilience Hub - AI Prediction Service"
    api_description: str = "AI-powered disaster risk prediction and evacuation planning"
    api_version: str = "1.0.0"

    # Performance Configuration
    max_concurrent_requests: int = 100
    request_timeout: int = 120  # seconds

    # Model paths
    models_dir: str = "models"
    data_dir: str = "data"
    logs_dir: str = "logs"

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"
        case_sensitive = False

    @property
    def cors_origins(self) -> list:
        """Parse CORS origins"""
        if self.allowed_origins == "*":
            return ["*"]
        return [origin.strip() for origin in self.allowed_origins.split(",")]

    @property
    def torch_device_type(self) -> str:
        """Determine PyTorch device"""
        import torch

        if self.torch_device:
            return self.torch_device

        if self.use_cuda and torch.cuda.is_available():
            return "cuda"

        return "cpu"

    def ensure_directories(self):
        """Ensure required directories exist"""
        for directory in [self.models_dir, self.data_dir, self.logs_dir]:
            os.makedirs(directory, exist_ok=True)


# Global settings instance
settings = Settings()


# Disaster type configurations
DISASTER_TYPES = {
    "earthquake": {
        "weight": 1.0,
        "urgency": "high",
        "typical_warning_time_hours": 0,  # No warning
        "primary_impacts": ["structural_damage", "casualties", "infrastructure"],
        "secondary_impacts": ["fires", "tsunamis", "landslides"]
    },
    "flood": {
        "weight": 0.9,
        "urgency": "medium",
        "typical_warning_time_hours": 24,
        "primary_impacts": ["water_damage", "infrastructure", "agriculture"],
        "secondary_impacts": ["disease_outbreak", "contamination"]
    },
    "wildfire": {
        "weight": 0.95,
        "urgency": "high",
        "typical_warning_time_hours": 6,
        "primary_impacts": ["property_damage", "casualties", "air_quality"],
        "secondary_impacts": ["erosion", "ecosystem_damage"]
    },
    "hurricane": {
        "weight": 1.0,
        "urgency": "critical",
        "typical_warning_time_hours": 72,
        "primary_impacts": ["wind_damage", "flooding", "infrastructure"],
        "secondary_impacts": ["power_outage", "supply_disruption"]
    },
    "tornado": {
        "weight": 1.0,
        "urgency": "critical",
        "typical_warning_time_hours": 0.5,
        "primary_impacts": ["structural_damage", "casualties", "debris"],
        "secondary_impacts": ["power_outage", "infrastructure"]
    },
    "tsunami": {
        "weight": 1.0,
        "urgency": "critical",
        "typical_warning_time_hours": 2,
        "primary_impacts": ["flooding", "casualties", "infrastructure"],
        "secondary_impacts": ["contamination", "debris", "disease"]
    },
    "landslide": {
        "weight": 0.85,
        "urgency": "high",
        "typical_warning_time_hours": 12,
        "primary_impacts": ["infrastructure", "casualties", "property_damage"],
        "secondary_impacts": ["road_blockage", "isolation"]
    },
    "drought": {
        "weight": 0.7,
        "urgency": "low",
        "typical_warning_time_hours": 720,  # 30 days
        "primary_impacts": ["agriculture", "water_supply", "economy"],
        "secondary_impacts": ["famine", "migration", "conflict"]
    }
}


# Risk level thresholds
RISK_THRESHOLDS = {
    "very_low": 0.2,
    "low": 0.4,
    "moderate": 0.6,
    "high": 0.8,
    "critical": 1.0
}


# Feature normalization ranges
FEATURE_RANGES = {
    "temperature": {"min": -40, "max": 50},
    "humidity": {"min": 0, "max": 100},
    "wind_speed": {"min": 0, "max": 200},
    "precipitation": {"min": 0, "max": 500},
    "seismic_activity": {"min": 0, "max": 10},
    "flood_risk": {"min": 0, "max": 10},
    "fire_danger_index": {"min": 0, "max": 10},
    "population_density": {"min": 0, "max": 20000},
    "infrastructure_quality": {"min": 0, "max": 10},
    "preparedness_level": {"min": 0, "max": 10},
    "vulnerable_population_ratio": {"min": 0, "max": 1},
    "emergency_services_capacity": {"min": 0, "max": 10},
    "hospital_capacity": {"min": 0, "max": 1},
    "medical_supply_level": {"min": 0, "max": 1},
    "disease_outbreak_risk": {"min": 0, "max": 10},
    "latitude": {"min": -90, "max": 90},
    "longitude": {"min": -180, "max": 180},
    "elevation": {"min": -500, "max": 9000},
    "coastal_proximity": {"min": 0, "max": 1000},
    "historical_disaster_count": {"min": 0, "max": 100}
}


# Resource requirement multipliers
RESOURCE_MULTIPLIERS = {
    "transport_vehicles_per_100_people": 2,
    "medical_personnel_per_1000_people": 5,
    "shelter_capacity_buffer": 1.2,  # 20% buffer
    "water_liters_per_person_per_day": 3,
    "food_meals_per_person_per_day": 3,
    "blankets_per_2_people": 1,
    "medical_kits_per_100_people": 1
}


# Evacuation priority thresholds
EVACUATION_PRIORITY_THRESHOLDS = {
    "immediate": 0.8,
    "high": 0.6,
    "medium": 0.4,
    "low": 0.0
}
