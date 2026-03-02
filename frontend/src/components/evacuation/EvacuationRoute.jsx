import { useEffect, useRef, useState } from 'react';
import { FaRoute, FaMapMarkerAlt, FaClock, FaRoad, FaCog } from 'react-icons/fa';

const EvacuationRoute = ({ route, onOptimize, onSave }) => {
  const mapContainerRef = useRef(null);
  const [optimizing, setOptimizing] = useState(false);

  useEffect(() => {
    // Initialize map (using a simple canvas for demonstration)
    // In production, this would integrate with Mapbox or similar
    if (mapContainerRef.current && route) {
      drawRoute();
    }
  }, [route]);

  const drawRoute = () => {
    const canvas = mapContainerRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    const width = canvas.width;
    const height = canvas.height;

    // Clear canvas
    ctx.fillStyle = '#1a1a1a';
    ctx.fillRect(0, 0, width, height);

    // Draw grid
    ctx.strokeStyle = '#333';
    ctx.lineWidth = 1;
    for (let i = 0; i < width; i += 50) {
      ctx.beginPath();
      ctx.moveTo(i, 0);
      ctx.lineTo(i, height);
      ctx.stroke();
    }
    for (let i = 0; i < height; i += 50) {
      ctx.beginPath();
      ctx.moveTo(0, i);
      ctx.lineTo(width, i);
      ctx.stroke();
    }

    if (!route.waypoints || route.waypoints.length < 2) return;

    // Scale waypoints to canvas
    const scaleX = width / 2;
    const scaleY = height / 2;
    const centerX = width / 2;
    const centerY = height / 2;

    const points = route.waypoints.map((wp) => ({
      x: centerX + wp.longitude * scaleX * 0.01,
      y: centerY - wp.latitude * scaleY * 0.01,
    }));

    // Draw route path
    ctx.strokeStyle = '#3b82f6';
    ctx.lineWidth = 3;
    ctx.beginPath();
    ctx.moveTo(points[0].x, points[0].y);
    for (let i = 1; i < points.length; i++) {
      ctx.lineTo(points[i].x, points[i].y);
    }
    ctx.stroke();

    // Draw waypoints
    points.forEach((point, index) => {
      ctx.fillStyle = index === 0 ? '#22c55e' : index === points.length - 1 ? '#ef4444' : '#3b82f6';
      ctx.beginPath();
      ctx.arc(point.x, point.y, 8, 0, Math.PI * 2);
      ctx.fill();

      // Draw labels
      ctx.fillStyle = '#fff';
      ctx.font = '12px sans-serif';
      ctx.fillText(
        index === 0 ? 'Start' : index === points.length - 1 ? 'End' : `${index}`,
        point.x + 12,
        point.y + 4
      );
    });
  };

  const handleOptimize = async () => {
    setOptimizing(true);
    await onOptimize(route.id);
    setOptimizing(false);
  };

  return (
    <div className="h-full flex flex-col">
      {/* Route Info Header */}
      <div className="bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 p-4">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-bold flex items-center">
            <FaRoute className="mr-2 text-primary-600" />
            {route.name || 'Evacuation Route'}
          </h2>
          <div className="flex gap-2">
            <button
              onClick={handleOptimize}
              disabled={optimizing}
              className="btn btn-secondary"
            >
              <FaCog className="mr-2" />
              {optimizing ? 'Optimizing...' : 'Optimize'}
            </button>
            <button onClick={onSave} className="btn btn-primary">
              Save Route
            </button>
          </div>
        </div>

        {/* Route Stats */}
        <div className="grid grid-cols-3 gap-4">
          <div className="bg-gray-50 dark:bg-gray-900 p-3 rounded-lg">
            <div className="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400 mb-1">
              <FaRoad />
              Distance
            </div>
            <div className="text-2xl font-bold">
              {route.distance?.toFixed(1) || 0} km
            </div>
          </div>
          <div className="bg-gray-50 dark:bg-gray-900 p-3 rounded-lg">
            <div className="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400 mb-1">
              <FaClock />
              Duration
            </div>
            <div className="text-2xl font-bold">
              {route.duration?.toFixed(0) || 0} min
            </div>
          </div>
          <div className="bg-gray-50 dark:bg-gray-900 p-3 rounded-lg">
            <div className="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400 mb-1">
              <FaMapMarkerAlt />
              Waypoints
            </div>
            <div className="text-2xl font-bold">
              {route.waypoints?.length || 0}
            </div>
          </div>
        </div>
      </div>

      {/* Map Canvas */}
      <div className="flex-1 relative bg-gray-900">
        <canvas
          ref={mapContainerRef}
          width={1200}
          height={800}
          className="w-full h-full"
        />

        {/* Safety Score */}
        {route.safety_score !== undefined && (
          <div className="absolute top-4 right-4 bg-white dark:bg-gray-800 rounded-lg shadow-lg p-4">
            <div className="text-sm text-gray-600 dark:text-gray-400 mb-1">
              Safety Score
            </div>
            <div className="flex items-center gap-2">
              <div className="flex-1 h-2 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden">
                <div
                  className={`h-full ${
                    route.safety_score >= 80
                      ? 'bg-success-500'
                      : route.safety_score >= 60
                      ? 'bg-warning-500'
                      : 'bg-danger-500'
                  }`}
                  style={{ width: `${route.safety_score}%` }}
                ></div>
              </div>
              <div className="text-lg font-bold">
                {route.safety_score.toFixed(0)}%
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Route Details */}
      {route.waypoints && route.waypoints.length > 0 && (
        <div className="bg-white dark:bg-gray-800 border-t border-gray-200 dark:border-gray-700 p-4 max-h-48 overflow-y-auto">
          <h3 className="font-bold mb-3">Route Details</h3>
          <div className="space-y-2">
            {route.waypoints.map((waypoint, index) => (
              <div
                key={index}
                className="flex items-start gap-3 text-sm"
              >
                <div
                  className={`w-6 h-6 rounded-full flex items-center justify-center text-white text-xs ${
                    index === 0
                      ? 'bg-success-500'
                      : index === route.waypoints.length - 1
                      ? 'bg-danger-500'
                      : 'bg-primary-500'
                  }`}
                >
                  {index + 1}
                </div>
                <div className="flex-1">
                  <div className="font-medium">
                    {waypoint.name || `Waypoint ${index + 1}`}
                  </div>
                  {waypoint.instructions && (
                    <div className="text-gray-600 dark:text-gray-400">
                      {waypoint.instructions}
                    </div>
                  )}
                  <div className="text-xs text-gray-500">
                    {waypoint.latitude?.toFixed(4)}, {waypoint.longitude?.toFixed(4)}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default EvacuationRoute;
