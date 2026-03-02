"""
Disaster Risk Prediction Logic
Handles risk assessment and evacuation route calculations
"""

import torch
import torch.nn as nn
import numpy as np
from typing import Dict, List, Tuple, Optional
from datetime import datetime
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class DisasterRiskModel(nn.Module):
    """PyTorch Neural Network for Disaster Risk Assessment"""

    def __init__(self, input_size: int = 20, hidden_sizes: List[int] = None):
        super(DisasterRiskModel, self).__init__()

        if hidden_sizes is None:
            hidden_sizes = [128, 64, 32]

        layers = []
        prev_size = input_size

        # Build hidden layers
        for hidden_size in hidden_sizes:
            layers.extend([
                nn.Linear(prev_size, hidden_size),
                nn.ReLU(),
                nn.BatchNorm1d(hidden_size),
                nn.Dropout(0.3)
            ])
            prev_size = hidden_size

        # Output layer - 5 risk categories
        layers.append(nn.Linear(prev_size, 5))
        layers.append(nn.Softmax(dim=1))

        self.network = nn.Sequential(*layers)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        return self.network(x)


class DisasterPredictor:
    """Main predictor class for disaster risk assessment"""

    def __init__(self, model_path: Optional[str] = None):
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.model = DisasterRiskModel().to(self.device)

        if model_path:
            try:
                self.model.load_state_dict(torch.load(model_path, map_location=self.device))
                self.model.eval()
                logger.info(f"Model loaded from {model_path}")
            except Exception as e:
                logger.warning(f"Could not load model from {model_path}: {e}")
                logger.info("Using untrained model")

        self.risk_categories = [
            "Very Low",
            "Low",
            "Moderate",
            "High",
            "Critical"
        ]

        self.disaster_types = {
            "earthquake": {"weight": 1.0, "urgency": "high"},
            "flood": {"weight": 0.9, "urgency": "medium"},
            "wildfire": {"weight": 0.95, "urgency": "high"},
            "hurricane": {"weight": 1.0, "urgency": "critical"},
            "tornado": {"weight": 1.0, "urgency": "critical"},
            "tsunami": {"weight": 1.0, "urgency": "critical"}
        }

    def preprocess_features(self, data: Dict) -> torch.Tensor:
        """Convert input data to feature tensor"""

        # Extract and normalize features
        features = []

        # Environmental features
        env_data = data.get("environmental_data", {})
        features.extend([
            env_data.get("temperature", 20) / 50.0,  # Normalize to 0-1
            env_data.get("humidity", 50) / 100.0,
            env_data.get("wind_speed", 0) / 100.0,
            env_data.get("precipitation", 0) / 200.0,
            env_data.get("seismic_activity", 0) / 10.0,
            env_data.get("flood_risk", 0) / 10.0,
            env_data.get("fire_danger_index", 0) / 10.0,
        ])

        # Community features
        community_data = data.get("community_data", {})
        features.extend([
            community_data.get("population_density", 100) / 10000.0,
            community_data.get("infrastructure_quality", 5) / 10.0,
            community_data.get("preparedness_level", 5) / 10.0,
            community_data.get("vulnerable_population_ratio", 0.2),
            community_data.get("emergency_services_capacity", 5) / 10.0,
        ])

        # Health features
        health_data = data.get("health_data", {})
        features.extend([
            health_data.get("hospital_capacity", 0.5),
            health_data.get("medical_supply_level", 0.7),
            health_data.get("disease_outbreak_risk", 0) / 10.0,
        ])

        # Location features
        location_data = data.get("location", {})
        features.extend([
            location_data.get("latitude", 0) / 90.0,
            location_data.get("longitude", 0) / 180.0,
            location_data.get("elevation", 0) / 5000.0,
            location_data.get("coastal_proximity", 0) / 100.0,
        ])

        # Historical disaster frequency
        features.append(data.get("historical_disaster_count", 0) / 50.0)

        # Pad or truncate to expected input size
        while len(features) < 20:
            features.append(0.0)
        features = features[:20]

        return torch.tensor(features, dtype=torch.float32).unsqueeze(0).to(self.device)

    def predict_risk(self, data: Dict) -> Dict:
        """Predict disaster risk based on input data"""

        try:
            # Preprocess input
            features = self.preprocess_features(data)

            # Make prediction
            with torch.no_grad():
                risk_probabilities = self.model(features)
                risk_scores = risk_probabilities.cpu().numpy()[0]

            # Get primary risk level
            primary_risk_idx = np.argmax(risk_scores)
            primary_risk_level = self.risk_categories[primary_risk_idx]
            confidence = float(risk_scores[primary_risk_idx])

            # Calculate overall risk score (weighted average)
            overall_risk_score = sum(
                (i + 1) * score for i, score in enumerate(risk_scores)
            ) / 5.0

            # Determine disaster types likely to occur
            disaster_type = data.get("disaster_type", "general")
            disaster_info = self.disaster_types.get(
                disaster_type.lower(),
                {"weight": 0.8, "urgency": "medium"}
            )

            # Calculate time to impact estimate
            time_to_impact = self._estimate_time_to_impact(
                overall_risk_score,
                disaster_info
            )

            result = {
                "risk_level": primary_risk_level,
                "risk_score": float(overall_risk_score),
                "confidence": confidence,
                "risk_breakdown": {
                    category: float(score)
                    for category, score in zip(self.risk_categories, risk_scores)
                },
                "disaster_type": disaster_type,
                "urgency": disaster_info["urgency"],
                "estimated_time_to_impact_hours": time_to_impact,
                "recommendations": self._generate_recommendations(
                    primary_risk_level,
                    disaster_type
                ),
                "timestamp": datetime.utcnow().isoformat()
            }

            logger.info(f"Risk prediction completed: {primary_risk_level} ({confidence:.2%})")
            return result

        except Exception as e:
            logger.error(f"Error in risk prediction: {e}")
            raise

    def _estimate_time_to_impact(self, risk_score: float, disaster_info: Dict) -> float:
        """Estimate time until disaster impact in hours"""

        base_time = 72.0  # 3 days base time
        urgency_multipliers = {
            "critical": 0.2,
            "high": 0.4,
            "medium": 0.6,
            "low": 0.8
        }

        multiplier = urgency_multipliers.get(disaster_info["urgency"], 0.6)
        time_to_impact = base_time * multiplier * (1.1 - risk_score)

        return max(1.0, time_to_impact)  # Minimum 1 hour

    def _generate_recommendations(self, risk_level: str, disaster_type: str) -> List[str]:
        """Generate safety recommendations based on risk level"""

        recommendations = []

        if risk_level in ["Critical", "High"]:
            recommendations.extend([
                "Activate emergency response protocols immediately",
                "Consider immediate evacuation of high-risk areas",
                "Alert all residents through emergency notification system",
                "Deploy emergency services to strategic locations",
                "Ensure emergency shelters are prepared and staffed"
            ])
        elif risk_level == "Moderate":
            recommendations.extend([
                "Monitor situation closely for changes",
                "Prepare evacuation plans and routes",
                "Alert vulnerable populations",
                "Check emergency supplies and equipment",
                "Coordinate with local emergency services"
            ])
        else:
            recommendations.extend([
                "Continue routine monitoring",
                "Review and update emergency plans",
                "Conduct community preparedness checks",
                "Maintain communication channels"
            ])

        # Add disaster-specific recommendations
        disaster_specific = {
            "earthquake": [
                "Secure heavy furniture and objects",
                "Identify safe spots in buildings",
                "Check structural integrity of buildings"
            ],
            "flood": [
                "Monitor water levels continuously",
                "Move valuables to higher ground",
                "Prepare sandbags and flood barriers"
            ],
            "wildfire": [
                "Create defensible space around properties",
                "Prepare evacuation bags",
                "Monitor air quality levels"
            ],
            "hurricane": [
                "Secure outdoor items and board windows",
                "Stock up on emergency supplies",
                "Review evacuation routes and shelters"
            ]
        }

        if disaster_type.lower() in disaster_specific:
            recommendations.extend(disaster_specific[disaster_type.lower()])

        return recommendations

    def generate_evacuation_plan(self, data: Dict) -> Dict:
        """Generate evacuation plan with route recommendations"""

        try:
            location = data.get("location", {})
            risk_assessment = self.predict_risk(data)

            # Calculate evacuation priority
            evacuation_priority = self._calculate_evacuation_priority(
                risk_assessment["risk_score"],
                data.get("population_at_risk", 1000)
            )

            # Generate evacuation routes (simplified - in production, use actual routing)
            routes = self._generate_evacuation_routes(location, data.get("destination", {}))

            # Calculate capacity requirements
            capacity_needs = self._calculate_capacity_needs(data)

            result = {
                "evacuation_priority": evacuation_priority,
                "risk_assessment": risk_assessment,
                "routes": routes,
                "capacity_requirements": capacity_needs,
                "estimated_evacuation_time_hours": self._estimate_evacuation_time(
                    data.get("population_at_risk", 1000),
                    len(routes)
                ),
                "assembly_points": self._identify_assembly_points(location),
                "shelter_locations": self._identify_shelters(location),
                "special_needs_considerations": self._get_special_needs_requirements(data),
                "timestamp": datetime.utcnow().isoformat()
            }

            logger.info(f"Evacuation plan generated with priority: {evacuation_priority}")
            return result

        except Exception as e:
            logger.error(f"Error generating evacuation plan: {e}")
            raise

    def _calculate_evacuation_priority(self, risk_score: float, population: int) -> str:
        """Calculate evacuation priority level"""

        priority_score = risk_score * (1 + np.log10(max(1, population)) / 10)

        if priority_score >= 0.8:
            return "IMMEDIATE"
        elif priority_score >= 0.6:
            return "HIGH"
        elif priority_score >= 0.4:
            return "MEDIUM"
        else:
            return "LOW"

    def _generate_evacuation_routes(
        self,
        origin: Dict,
        destination: Dict
    ) -> List[Dict]:
        """Generate multiple evacuation routes"""

        # Simplified route generation - in production, integrate with mapping service
        routes = []

        origin_lat = origin.get("latitude", 0)
        origin_lon = origin.get("longitude", 0)
        dest_lat = destination.get("latitude", origin_lat + 0.5)
        dest_lon = destination.get("longitude", origin_lon + 0.5)

        # Primary route
        routes.append({
            "route_id": "primary",
            "name": "Primary Evacuation Route",
            "start": {"latitude": origin_lat, "longitude": origin_lon},
            "end": {"latitude": dest_lat, "longitude": dest_lon},
            "distance_km": self._calculate_distance(origin_lat, origin_lon, dest_lat, dest_lon),
            "estimated_time_minutes": 45,
            "capacity": "high",
            "road_conditions": "good",
            "waypoints": []
        })

        # Alternate route
        routes.append({
            "route_id": "alternate_1",
            "name": "Alternate Route 1",
            "start": {"latitude": origin_lat, "longitude": origin_lon},
            "end": {"latitude": dest_lat + 0.1, "longitude": dest_lon - 0.1},
            "distance_km": self._calculate_distance(origin_lat, origin_lon, dest_lat + 0.1, dest_lon - 0.1),
            "estimated_time_minutes": 60,
            "capacity": "medium",
            "road_conditions": "fair",
            "waypoints": []
        })

        # Emergency route
        routes.append({
            "route_id": "emergency",
            "name": "Emergency Route",
            "start": {"latitude": origin_lat, "longitude": origin_lon},
            "end": {"latitude": dest_lat - 0.1, "longitude": dest_lon + 0.1},
            "distance_km": self._calculate_distance(origin_lat, origin_lon, dest_lat - 0.1, dest_lon + 0.1),
            "estimated_time_minutes": 50,
            "capacity": "medium",
            "road_conditions": "good",
            "waypoints": []
        })

        return routes

    def _calculate_distance(self, lat1: float, lon1: float, lat2: float, lon2: float) -> float:
        """Calculate distance between two coordinates (Haversine formula)"""

        R = 6371  # Earth's radius in km

        lat1_rad = np.radians(lat1)
        lat2_rad = np.radians(lat2)
        delta_lat = np.radians(lat2 - lat1)
        delta_lon = np.radians(lon2 - lon1)

        a = np.sin(delta_lat/2)**2 + np.cos(lat1_rad) * np.cos(lat2_rad) * np.sin(delta_lon/2)**2
        c = 2 * np.arctan2(np.sqrt(a), np.sqrt(1-a))

        return R * c

    def _calculate_capacity_needs(self, data: Dict) -> Dict:
        """Calculate evacuation capacity requirements"""

        population = data.get("population_at_risk", 1000)
        vulnerable_ratio = data.get("community_data", {}).get("vulnerable_population_ratio", 0.2)

        return {
            "total_people": population,
            "vulnerable_individuals": int(population * vulnerable_ratio),
            "transport_vehicles_needed": max(1, population // 50),
            "shelter_capacity_needed": int(population * 1.2),  # 20% buffer
            "medical_personnel_needed": max(2, population // 200),
            "supplies_needed": {
                "water_liters": population * 3,  # 3L per person
                "food_meals": population * 3,  # 3 meals per person
                "blankets": int(population * 0.5),
                "medical_kits": max(5, population // 100)
            }
        }

    def _estimate_evacuation_time(self, population: int, num_routes: int) -> float:
        """Estimate total evacuation time in hours"""

        base_time_per_person = 0.01  # hours
        route_efficiency = min(1.0, num_routes / 3.0)

        evacuation_time = (population * base_time_per_person) / route_efficiency

        return max(1.0, evacuation_time)

    def _identify_assembly_points(self, location: Dict) -> List[Dict]:
        """Identify safe assembly points"""

        lat = location.get("latitude", 0)
        lon = location.get("longitude", 0)

        return [
            {
                "id": "ap_1",
                "name": "Primary Assembly Point",
                "location": {"latitude": lat + 0.01, "longitude": lon + 0.01},
                "capacity": 500,
                "facilities": ["parking", "restrooms", "shelter"]
            },
            {
                "id": "ap_2",
                "name": "Secondary Assembly Point",
                "location": {"latitude": lat - 0.01, "longitude": lon + 0.01},
                "capacity": 300,
                "facilities": ["parking", "medical"]
            }
        ]

    def _identify_shelters(self, location: Dict) -> List[Dict]:
        """Identify emergency shelter locations"""

        lat = location.get("latitude", 0)
        lon = location.get("longitude", 0)

        return [
            {
                "id": "shelter_1",
                "name": "Community Center Shelter",
                "location": {"latitude": lat + 0.05, "longitude": lon + 0.05},
                "capacity": 1000,
                "facilities": ["food", "water", "medical", "power", "communications"],
                "accessibility": "wheelchair_accessible"
            },
            {
                "id": "shelter_2",
                "name": "School Gymnasium Shelter",
                "location": {"latitude": lat + 0.03, "longitude": lon - 0.03},
                "capacity": 500,
                "facilities": ["food", "water", "restrooms"],
                "accessibility": "wheelchair_accessible"
            }
        ]

    def _get_special_needs_requirements(self, data: Dict) -> Dict:
        """Get special needs considerations for evacuation"""

        return {
            "mobility_assistance_needed": True,
            "medical_transport_required": True,
            "language_support": ["English", "Spanish", "Mandarin"],
            "pet_accommodation": True,
            "medical_equipment_power": True,
            "dietary_restrictions": True
        }
