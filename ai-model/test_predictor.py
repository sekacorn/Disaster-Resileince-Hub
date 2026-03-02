"""
Pytest Tests for Disaster Prediction Service
Tests API endpoints and prediction logic
"""

import pytest
import sys
from pathlib import Path
from fastapi.testclient import TestClient
import torch
import numpy as np

# Add parent directory to path for imports
sys.path.insert(0, str(Path(__file__).parent))

from disaster_predictor import app
from predictor import DisasterPredictor, DisasterRiskModel


# Test client
client = TestClient(app)


# Fixtures
@pytest.fixture
def sample_risk_request():
    """Sample risk prediction request"""
    return {
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


@pytest.fixture
def sample_evacuation_request():
    """Sample evacuation request"""
    return {
        "location": {
            "latitude": 37.7749,
            "longitude": -122.4194,
            "elevation": 16
        },
        "destination": {
            "latitude": 37.8749,
            "longitude": -122.3194,
            "elevation": 50
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
            "vulnerable_population_ratio": 0.25
        }
    }


@pytest.fixture
def predictor():
    """Create predictor instance"""
    return DisasterPredictor()


# API Endpoint Tests
class TestAPIEndpoints:
    """Test FastAPI endpoints"""

    def test_root_endpoint(self):
        """Test root endpoint"""
        response = client.get("/")
        assert response.status_code == 200
        data = response.json()
        assert "service" in data
        assert "version" in data
        assert data["status"] == "running"

    def test_health_check(self):
        """Test health check endpoint"""
        response = client.get("/health")
        assert response.status_code == 200
        data = response.json()
        assert "status" in data
        assert "timestamp" in data
        assert "model_loaded" in data
        assert "device" in data
        assert data["status"] in ["healthy", "degraded"]

    def test_resources_check(self):
        """Test system resources endpoint"""
        response = client.get("/resources/check")
        assert response.status_code == 200
        data = response.json()
        assert "cpu_count" in data
        assert "cpu_percent" in data
        assert "memory_total_gb" in data
        assert "memory_available_gb" in data
        assert "memory_percent" in data
        assert "recommended_workers" in data
        assert data["cpu_count"] > 0
        assert data["recommended_workers"] > 0

    def test_predict_risk_endpoint(self, sample_risk_request):
        """Test risk prediction endpoint"""
        response = client.post("/predict/risk", json=sample_risk_request)
        assert response.status_code == 200
        data = response.json()

        # Check response structure
        assert "risk_level" in data
        assert "risk_score" in data
        assert "confidence" in data
        assert "risk_breakdown" in data
        assert "disaster_type" in data
        assert "urgency" in data
        assert "recommendations" in data
        assert "timestamp" in data

        # Validate data types
        assert isinstance(data["risk_level"], str)
        assert isinstance(data["risk_score"], float)
        assert isinstance(data["confidence"], float)
        assert isinstance(data["risk_breakdown"], dict)
        assert isinstance(data["recommendations"], list)

        # Validate ranges
        assert 0 <= data["risk_score"] <= 1
        assert 0 <= data["confidence"] <= 1
        assert data["risk_level"] in ["Very Low", "Low", "Moderate", "High", "Critical"]
        assert data["urgency"] in ["low", "medium", "high", "critical"]

    def test_predict_evacuation_endpoint(self, sample_evacuation_request):
        """Test evacuation prediction endpoint"""
        response = client.post("/predict/evacuation", json=sample_evacuation_request)
        assert response.status_code == 200
        data = response.json()

        # Check response structure
        assert "evacuation_priority" in data
        assert "risk_assessment" in data
        assert "routes" in data
        assert "capacity_requirements" in data
        assert "assembly_points" in data
        assert "shelter_locations" in data
        assert "estimated_evacuation_time_hours" in data

        # Validate evacuation priority
        assert data["evacuation_priority"] in ["LOW", "MEDIUM", "HIGH", "IMMEDIATE"]

        # Validate routes
        assert isinstance(data["routes"], list)
        assert len(data["routes"]) > 0
        for route in data["routes"]:
            assert "route_id" in route
            assert "distance_km" in route
            assert "estimated_time_minutes" in route

        # Validate capacity requirements
        capacity = data["capacity_requirements"]
        assert "total_people" in capacity
        assert "transport_vehicles_needed" in capacity
        assert "shelter_capacity_needed" in capacity

        # Validate assembly points and shelters
        assert isinstance(data["assembly_points"], list)
        assert isinstance(data["shelter_locations"], list)

    def test_predict_risk_invalid_data(self):
        """Test risk prediction with invalid data"""
        invalid_request = {
            "location": {
                "latitude": 200,  # Invalid latitude
                "longitude": -122.4194
            },
            "disaster_type": "earthquake"
        }
        response = client.post("/predict/risk", json=invalid_request)
        assert response.status_code == 422  # Validation error

    def test_predict_risk_missing_required_fields(self):
        """Test risk prediction with missing required fields"""
        invalid_request = {
            "location": {
                "latitude": 37.7749
                # Missing longitude
            }
        }
        response = client.post("/predict/risk", json=invalid_request)
        assert response.status_code == 422


# Predictor Logic Tests
class TestPredictorLogic:
    """Test predictor business logic"""

    def test_predictor_initialization(self):
        """Test predictor initialization"""
        predictor = DisasterPredictor()
        assert predictor is not None
        assert predictor.model is not None
        assert predictor.device is not None
        assert len(predictor.risk_categories) == 5
        assert len(predictor.disaster_types) > 0

    def test_preprocess_features(self, predictor, sample_risk_request):
        """Test feature preprocessing"""
        features = predictor.preprocess_features(sample_risk_request)

        assert isinstance(features, torch.Tensor)
        assert features.shape[0] == 1  # Batch size
        assert features.shape[1] == 20  # Feature size
        assert torch.all(features >= 0)  # All features should be normalized to positive values
        assert torch.all(features <= 1)  # Most features normalized to 0-1 range

    def test_predict_risk(self, predictor, sample_risk_request):
        """Test risk prediction logic"""
        result = predictor.predict_risk(sample_risk_request)

        assert isinstance(result, dict)
        assert "risk_level" in result
        assert "risk_score" in result
        assert "confidence" in result
        assert "recommendations" in result

        assert result["risk_level"] in predictor.risk_categories
        assert 0 <= result["risk_score"] <= 1
        assert 0 <= result["confidence"] <= 1
        assert isinstance(result["recommendations"], list)
        assert len(result["recommendations"]) > 0

    def test_generate_evacuation_plan(self, predictor, sample_evacuation_request):
        """Test evacuation plan generation"""
        result = predictor.generate_evacuation_plan(sample_evacuation_request)

        assert isinstance(result, dict)
        assert "evacuation_priority" in result
        assert "routes" in result
        assert "capacity_requirements" in result

        assert result["evacuation_priority"] in ["LOW", "MEDIUM", "HIGH", "IMMEDIATE"]
        assert isinstance(result["routes"], list)
        assert len(result["routes"]) > 0

    def test_calculate_distance(self, predictor):
        """Test distance calculation"""
        # San Francisco to Los Angeles (approximately 560 km)
        distance = predictor._calculate_distance(37.7749, -122.4194, 34.0522, -118.2437)

        assert isinstance(distance, float)
        assert distance > 0
        assert 500 < distance < 650  # Approximate range

    def test_evacuation_priority_calculation(self, predictor):
        """Test evacuation priority calculation"""
        # High risk, high population
        priority1 = predictor._calculate_evacuation_priority(0.9, 10000)
        assert priority1 in ["HIGH", "IMMEDIATE"]

        # Low risk, low population
        priority2 = predictor._calculate_evacuation_priority(0.2, 100)
        assert priority2 in ["LOW", "MEDIUM"]

    def test_recommendations_generation(self, predictor):
        """Test recommendations generation"""
        # Critical risk
        recommendations1 = predictor._generate_recommendations("Critical", "earthquake")
        assert len(recommendations1) > 0
        assert any("emergency" in rec.lower() for rec in recommendations1)

        # Low risk
        recommendations2 = predictor._generate_recommendations("Low", "flood")
        assert len(recommendations2) > 0


# Model Tests
class TestDisasterRiskModel:
    """Test PyTorch model"""

    def test_model_creation(self):
        """Test model creation"""
        model = DisasterRiskModel(input_size=20, hidden_sizes=[128, 64, 32])
        assert model is not None
        assert isinstance(model, torch.nn.Module)

    def test_model_forward_pass(self):
        """Test model forward pass"""
        model = DisasterRiskModel(input_size=20)
        model.eval()

        # Create dummy input
        x = torch.randn(5, 20)  # Batch of 5 samples

        with torch.no_grad():
            output = model(x)

        # Check output shape
        assert output.shape == (5, 5)  # 5 samples, 5 risk categories

        # Check output is probability distribution
        assert torch.allclose(output.sum(dim=1), torch.ones(5), atol=1e-5)
        assert torch.all(output >= 0)
        assert torch.all(output <= 1)

    def test_model_output_range(self):
        """Test model output is in valid range"""
        model = DisasterRiskModel(input_size=20)
        model.eval()

        # Test with different inputs
        for _ in range(10):
            x = torch.rand(1, 20)
            with torch.no_grad():
                output = model(x)

            # Should be valid probability distribution
            assert torch.allclose(output.sum(), torch.ones(1), atol=1e-5)
            assert torch.all(output >= 0)
            assert torch.all(output <= 1)


# Integration Tests
class TestIntegration:
    """Integration tests"""

    def test_end_to_end_risk_prediction(self, sample_risk_request):
        """Test complete risk prediction flow"""
        response = client.post("/predict/risk", json=sample_risk_request)
        assert response.status_code == 200

        data = response.json()
        assert data["disaster_type"] == sample_risk_request["disaster_type"]

        # Check that recommendations are relevant
        assert len(data["recommendations"]) > 0

    def test_end_to_end_evacuation(self, sample_evacuation_request):
        """Test complete evacuation planning flow"""
        response = client.post("/predict/evacuation", json=sample_evacuation_request)
        assert response.status_code == 200

        data = response.json()
        assert data["capacity_requirements"]["total_people"] == sample_evacuation_request["population_at_risk"]

        # Check routes are generated
        assert len(data["routes"]) > 0
        assert len(data["shelter_locations"]) > 0

    def test_multiple_disaster_types(self, sample_risk_request):
        """Test predictions for different disaster types"""
        disaster_types = ["earthquake", "flood", "wildfire", "hurricane", "tornado"]

        for disaster_type in disaster_types:
            request = sample_risk_request.copy()
            request["disaster_type"] = disaster_type

            response = client.post("/predict/risk", json=request)
            assert response.status_code == 200

            data = response.json()
            assert data["disaster_type"] == disaster_type


# Performance Tests
class TestPerformance:
    """Performance tests"""

    def test_prediction_response_time(self, sample_risk_request):
        """Test prediction response time"""
        import time

        start_time = time.time()
        response = client.post("/predict/risk", json=sample_risk_request)
        end_time = time.time()

        assert response.status_code == 200
        response_time = end_time - start_time

        # Should respond in less than 2 seconds
        assert response_time < 2.0

    def test_concurrent_requests(self, sample_risk_request):
        """Test handling multiple concurrent requests"""
        import concurrent.futures

        def make_request():
            return client.post("/predict/risk", json=sample_risk_request)

        # Make 10 concurrent requests
        with concurrent.futures.ThreadPoolExecutor(max_workers=10) as executor:
            futures = [executor.submit(make_request) for _ in range(10)]
            results = [f.result() for f in concurrent.futures.as_completed(futures)]

        # All requests should succeed
        assert all(r.status_code == 200 for r in results)


if __name__ == "__main__":
    pytest.main([__file__, "-v", "--tb=short"])
