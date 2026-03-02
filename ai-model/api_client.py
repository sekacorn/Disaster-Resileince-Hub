"""
API Client for Disaster Prediction Service
Example usage and testing utilities
"""

import httpx
import asyncio
from typing import Dict, Optional
import json
from datetime import datetime


class DisasterPredictionClient:
    """Client for interacting with the Disaster Prediction API"""

    def __init__(self, base_url: str = "http://localhost:8000"):
        self.base_url = base_url.rstrip("/")
        self.timeout = httpx.Timeout(30.0)

    async def health_check(self) -> Dict:
        """Check service health"""
        async with httpx.AsyncClient(timeout=self.timeout) as client:
            response = await client.get(f"{self.base_url}/health")
            response.raise_for_status()
            return response.json()

    async def check_resources(self) -> Dict:
        """Check system resources"""
        async with httpx.AsyncClient(timeout=self.timeout) as client:
            response = await client.get(f"{self.base_url}/resources/check")
            response.raise_for_status()
            return response.json()

    async def predict_risk(
        self,
        location: Dict,
        disaster_type: str,
        environmental_data: Optional[Dict] = None,
        community_data: Optional[Dict] = None,
        health_data: Optional[Dict] = None,
        historical_disaster_count: int = 0
    ) -> Dict:
        """Predict disaster risk"""

        payload = {
            "location": location,
            "disaster_type": disaster_type,
            "historical_disaster_count": historical_disaster_count
        }

        if environmental_data:
            payload["environmental_data"] = environmental_data
        if community_data:
            payload["community_data"] = community_data
        if health_data:
            payload["health_data"] = health_data

        async with httpx.AsyncClient(timeout=self.timeout) as client:
            response = await client.post(
                f"{self.base_url}/predict/risk",
                json=payload
            )
            response.raise_for_status()
            return response.json()

    async def generate_evacuation_plan(
        self,
        location: Dict,
        disaster_type: str,
        population_at_risk: int,
        destination: Optional[Dict] = None,
        environmental_data: Optional[Dict] = None,
        community_data: Optional[Dict] = None,
        health_data: Optional[Dict] = None,
        historical_disaster_count: int = 0
    ) -> Dict:
        """Generate evacuation plan"""

        payload = {
            "location": location,
            "disaster_type": disaster_type,
            "population_at_risk": population_at_risk,
            "historical_disaster_count": historical_disaster_count
        }

        if destination:
            payload["destination"] = destination
        if environmental_data:
            payload["environmental_data"] = environmental_data
        if community_data:
            payload["community_data"] = community_data
        if health_data:
            payload["health_data"] = health_data

        async with httpx.AsyncClient(timeout=self.timeout) as client:
            response = await client.post(
                f"{self.base_url}/predict/evacuation",
                json=payload
            )
            response.raise_for_status()
            return response.json()

    def predict_risk_sync(self, **kwargs) -> Dict:
        """Synchronous wrapper for predict_risk"""
        return asyncio.run(self.predict_risk(**kwargs))

    def generate_evacuation_plan_sync(self, **kwargs) -> Dict:
        """Synchronous wrapper for generate_evacuation_plan"""
        return asyncio.run(self.generate_evacuation_plan(**kwargs))


# Example usage functions
async def example_earthquake_prediction():
    """Example: Predict earthquake risk"""

    client = DisasterPredictionClient()

    print("=" * 60)
    print("Example: Earthquake Risk Prediction")
    print("=" * 60)

    # San Francisco coordinates
    location = {
        "latitude": 37.7749,
        "longitude": -122.4194,
        "elevation": 16,
        "coastal_proximity": 5
    }

    environmental_data = {
        "temperature": 18,
        "humidity": 65,
        "wind_speed": 15,
        "seismic_activity": 6.5,
        "flood_risk": 2.0,
        "fire_danger_index": 3.0
    }

    community_data = {
        "population_density": 7200,
        "infrastructure_quality": 7,
        "preparedness_level": 6,
        "vulnerable_population_ratio": 0.15,
        "emergency_services_capacity": 8
    }

    health_data = {
        "hospital_capacity": 0.7,
        "medical_supply_level": 0.8,
        "disease_outbreak_risk": 1.0
    }

    result = await client.predict_risk(
        location=location,
        disaster_type="earthquake",
        environmental_data=environmental_data,
        community_data=community_data,
        health_data=health_data,
        historical_disaster_count=12
    )

    print(f"\nRisk Level: {result['risk_level']}")
    print(f"Risk Score: {result['risk_score']:.2%}")
    print(f"Confidence: {result['confidence']:.2%}")
    print(f"Urgency: {result['urgency']}")
    print(f"Time to Impact: {result['estimated_time_to_impact_hours']:.1f} hours")
    print("\nRecommendations:")
    for i, rec in enumerate(result['recommendations'], 1):
        print(f"  {i}. {rec}")

    return result


async def example_wildfire_evacuation():
    """Example: Generate wildfire evacuation plan"""

    client = DisasterPredictionClient()

    print("\n" + "=" * 60)
    print("Example: Wildfire Evacuation Plan")
    print("=" * 60)

    # Los Angeles coordinates
    location = {
        "latitude": 34.0522,
        "longitude": -118.2437,
        "elevation": 71
    }

    destination = {
        "latitude": 34.1522,
        "longitude": -118.1437,
        "elevation": 100
    }

    environmental_data = {
        "temperature": 38,
        "humidity": 15,
        "wind_speed": 55,
        "fire_danger_index": 9.2
    }

    community_data = {
        "population_density": 3200,
        "vulnerable_population_ratio": 0.22,
        "preparedness_level": 7
    }

    result = await client.generate_evacuation_plan(
        location=location,
        disaster_type="wildfire",
        population_at_risk=8000,
        destination=destination,
        environmental_data=environmental_data,
        community_data=community_data
    )

    print(f"\nEvacuation Priority: {result['evacuation_priority']}")
    print(f"Risk Level: {result['risk_assessment']['risk_level']}")
    print(f"Estimated Evacuation Time: {result['estimated_evacuation_time_hours']:.1f} hours")

    print(f"\nEvacuation Routes ({len(result['routes'])} routes):")
    for route in result['routes']:
        print(f"  - {route['name']}: {route['distance_km']:.1f} km, "
              f"{route['estimated_time_minutes']} minutes")

    print(f"\nShelter Locations ({len(result['shelter_locations'])} shelters):")
    for shelter in result['shelter_locations']:
        print(f"  - {shelter['name']}: Capacity {shelter['capacity']} people")

    print("\nCapacity Requirements:")
    capacity = result['capacity_requirements']
    print(f"  - Total People: {capacity['total_people']}")
    print(f"  - Transport Vehicles: {capacity['transport_vehicles_needed']}")
    print(f"  - Shelter Capacity: {capacity['shelter_capacity_needed']}")
    print(f"  - Medical Personnel: {capacity['medical_personnel_needed']}")

    return result


async def example_hurricane_monitoring():
    """Example: Monitor hurricane risk"""

    client = DisasterPredictionClient()

    print("\n" + "=" * 60)
    print("Example: Hurricane Risk Monitoring")
    print("=" * 60)

    # Miami coordinates
    location = {
        "latitude": 25.7617,
        "longitude": -80.1918,
        "elevation": 2,
        "coastal_proximity": 1
    }

    environmental_data = {
        "temperature": 28,
        "humidity": 85,
        "wind_speed": 120,
        "precipitation": 150,
        "flood_risk": 8.5
    }

    community_data = {
        "population_density": 4800,
        "infrastructure_quality": 6,
        "preparedness_level": 8,
        "vulnerable_population_ratio": 0.18
    }

    result = await client.predict_risk(
        location=location,
        disaster_type="hurricane",
        environmental_data=environmental_data,
        community_data=community_data,
        historical_disaster_count=25
    )

    print(f"\nRisk Level: {result['risk_level']}")
    print(f"Risk Score: {result['risk_score']:.2%}")
    print(f"Urgency: {result['urgency']}")
    print(f"Time to Impact: {result['estimated_time_to_impact_hours']:.1f} hours")

    print("\nRisk Breakdown:")
    for category, score in result['risk_breakdown'].items():
        print(f"  - {category}: {score:.2%}")

    return result


async def example_health_check():
    """Example: Check service health and resources"""

    client = DisasterPredictionClient()

    print("\n" + "=" * 60)
    print("Service Health Check")
    print("=" * 60)

    health = await client.health_check()
    print(f"\nStatus: {health['status']}")
    print(f"Model Loaded: {health['model_loaded']}")
    print(f"Device: {health['device']}")

    print("\n" + "=" * 60)
    print("System Resources")
    print("=" * 60)

    resources = await client.check_resources()
    print(f"\nCPU Count: {resources['cpu_count']}")
    print(f"CPU Usage: {resources['cpu_percent']:.1f}%")
    print(f"Memory Total: {resources['memory_total_gb']:.2f} GB")
    print(f"Memory Available: {resources['memory_available_gb']:.2f} GB")
    print(f"Memory Usage: {resources['memory_percent']:.1f}%")
    print(f"Recommended Workers: {resources['recommended_workers']}")

    return health, resources


async def run_all_examples():
    """Run all examples"""

    try:
        # Health check
        await example_health_check()

        # Risk predictions
        await example_earthquake_prediction()
        await example_hurricane_monitoring()

        # Evacuation planning
        await example_wildfire_evacuation()

        print("\n" + "=" * 60)
        print("All examples completed successfully!")
        print("=" * 60)

    except httpx.HTTPError as e:
        print(f"\nError: {e}")
        print("Make sure the service is running on http://localhost:8000")
    except Exception as e:
        print(f"\nUnexpected error: {e}")


def main():
    """Main entry point"""
    print("Disaster Prediction Service - API Client Examples")
    print()

    try:
        asyncio.run(run_all_examples())
    except KeyboardInterrupt:
        print("\n\nInterrupted by user")


if __name__ == "__main__":
    main()
