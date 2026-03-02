import { useState, useEffect } from 'react';
import { evacuationAPI } from '@services/api';
import EvacuationRoute from '@components/evacuation/EvacuationRoute';
import { toast } from 'react-toastify';
import { FaRoute, FaMapMarkerAlt, FaUsers, FaSave } from 'react-icons/fa';

const EvacuationPlanner = () => {
  const [routes, setRoutes] = useState([]);
  const [selectedRoute, setSelectedRoute] = useState(null);
  const [loading, setLoading] = useState(false);
  const [plannerData, setPlannerData] = useState({
    startLocation: '',
    startLat: '',
    startLng: '',
    endLocation: '',
    endLat: '',
    endLng: '',
    evacuees: '',
    vehicleType: 'car',
    disasterType: 'general',
    avoidAreas: [],
  });

  useEffect(() => {
    loadRoutes();
  }, []);

  const loadRoutes = async () => {
    try {
      const response = await evacuationAPI.getRoutes();
      setRoutes(response.data.routes || []);
    } catch (error) {
      toast.error('Failed to load routes');
    }
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setPlannerData({ ...plannerData, [name]: value });
  };

  const handlePlanRoute = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      const response = await evacuationAPI.planRoute({
        start_location: {
          name: plannerData.startLocation,
          latitude: parseFloat(plannerData.startLat),
          longitude: parseFloat(plannerData.startLng),
        },
        end_location: {
          name: plannerData.endLocation,
          latitude: parseFloat(plannerData.endLat),
          longitude: parseFloat(plannerData.endLng),
        },
        evacuees_count: parseInt(plannerData.evacuees),
        vehicle_type: plannerData.vehicleType,
        disaster_type: plannerData.disasterType,
        avoid_areas: plannerData.avoidAreas,
      });

      setSelectedRoute(response.data);
      toast.success('Route planned successfully');
      loadRoutes();
    } catch (error) {
      toast.error('Failed to plan route');
    } finally {
      setLoading(false);
    }
  };

  const handleOptimizeRoute = async (routeId) => {
    try {
      const response = await evacuationAPI.optimizeRoute(routeId);
      setSelectedRoute(response.data);
      toast.success('Route optimized successfully');
    } catch (error) {
      toast.error('Failed to optimize route');
    }
  };

  const handleSaveRoute = () => {
    toast.success('Route saved successfully');
    loadRoutes();
  };

  return (
    <div className="h-full flex">
      {/* Left Panel - Route Planner */}
      <div className="w-96 bg-white dark:bg-gray-800 border-r border-gray-200 dark:border-gray-700 p-6 overflow-y-auto">
        <h1 className="text-2xl font-bold mb-6 flex items-center">
          <FaRoute className="mr-2 text-primary-600" />
          Evacuation Planner
        </h1>

        <form onSubmit={handlePlanRoute} className="space-y-4">
          {/* Start Location */}
          <div className="card bg-gray-50 dark:bg-gray-900">
            <h3 className="font-bold mb-3 flex items-center">
              <FaMapMarkerAlt className="mr-2 text-success-500" />
              Start Location
            </h3>
            <input
              type="text"
              name="startLocation"
              placeholder="Location name"
              value={plannerData.startLocation}
              onChange={handleInputChange}
              className="input mb-2"
              required
            />
            <div className="grid grid-cols-2 gap-2">
              <input
                type="number"
                step="0.000001"
                name="startLat"
                placeholder="Latitude"
                value={plannerData.startLat}
                onChange={handleInputChange}
                className="input"
                required
              />
              <input
                type="number"
                step="0.000001"
                name="startLng"
                placeholder="Longitude"
                value={plannerData.startLng}
                onChange={handleInputChange}
                className="input"
                required
              />
            </div>
          </div>

          {/* End Location */}
          <div className="card bg-gray-50 dark:bg-gray-900">
            <h3 className="font-bold mb-3 flex items-center">
              <FaMapMarkerAlt className="mr-2 text-danger-500" />
              Destination
            </h3>
            <input
              type="text"
              name="endLocation"
              placeholder="Location name"
              value={plannerData.endLocation}
              onChange={handleInputChange}
              className="input mb-2"
              required
            />
            <div className="grid grid-cols-2 gap-2">
              <input
                type="number"
                step="0.000001"
                name="endLat"
                placeholder="Latitude"
                value={plannerData.endLat}
                onChange={handleInputChange}
                className="input"
                required
              />
              <input
                type="number"
                step="0.000001"
                name="endLng"
                placeholder="Longitude"
                value={plannerData.endLng}
                onChange={handleInputChange}
                className="input"
                required
              />
            </div>
          </div>

          {/* Evacuation Details */}
          <div>
            <label className="block text-sm font-medium mb-2">
              <FaUsers className="inline mr-2" />
              Number of Evacuees
            </label>
            <input
              type="number"
              name="evacuees"
              placeholder="0"
              value={plannerData.evacuees}
              onChange={handleInputChange}
              className="input"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium mb-2">
              Vehicle Type
            </label>
            <select
              name="vehicleType"
              value={plannerData.vehicleType}
              onChange={handleInputChange}
              className="input"
            >
              <option value="car">Car</option>
              <option value="bus">Bus</option>
              <option value="truck">Truck</option>
              <option value="walking">Walking</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium mb-2">
              Disaster Type
            </label>
            <select
              name="disasterType"
              value={plannerData.disasterType}
              onChange={handleInputChange}
              className="input"
            >
              <option value="general">General</option>
              <option value="flood">Flood</option>
              <option value="wildfire">Wildfire</option>
              <option value="earthquake">Earthquake</option>
              <option value="hurricane">Hurricane</option>
            </select>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="btn btn-primary w-full"
          >
            {loading ? 'Planning...' : 'Plan Route'}
          </button>
        </form>

        {/* Saved Routes */}
        <div className="mt-6">
          <h3 className="font-bold mb-3">Saved Routes</h3>
          {routes.length > 0 ? (
            <div className="space-y-2">
              {routes.map((route) => (
                <div
                  key={route.id}
                  onClick={() => setSelectedRoute(route)}
                  className={`p-3 rounded-lg border cursor-pointer transition-colors ${
                    selectedRoute?.id === route.id
                      ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20'
                      : 'border-gray-200 dark:border-gray-700 hover:border-primary-300'
                  }`}
                >
                  <div className="font-medium">{route.name || 'Unnamed Route'}</div>
                  <div className="text-xs text-gray-500">
                    {route.distance?.toFixed(1)} km • {route.duration?.toFixed(0)} min
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-sm text-gray-500">No saved routes</p>
          )}
        </div>
      </div>

      {/* Right Panel - Map View */}
      <div className="flex-1 relative">
        {selectedRoute ? (
          <EvacuationRoute
            route={selectedRoute}
            onOptimize={handleOptimizeRoute}
            onSave={handleSaveRoute}
          />
        ) : (
          <div className="flex items-center justify-center h-full text-gray-500">
            <div className="text-center">
              <FaRoute className="text-6xl mb-4 mx-auto opacity-20" />
              <p>Plan a route to see it here</p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default EvacuationPlanner;
