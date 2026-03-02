import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import DisasterViewer3D from '@components/visualization/DisasterViewer3D';
import { disasterAPI } from '@services/api';
import { toast } from 'react-toastify';
import { FaFilter, FaDownload, FaExpand } from 'react-icons/fa';

const DisasterMap = () => {
  const [searchParams] = useSearchParams();
  const [disasters, setDisasters] = useState([]);
  const [selectedDisaster, setSelectedDisaster] = useState(null);
  const [filters, setFilters] = useState({
    type: 'all',
    severity: 'all',
    dateRange: '30',
  });
  const [loading, setLoading] = useState(true);
  const [showFilters, setShowFilters] = useState(false);

  useEffect(() => {
    loadDisasters();
  }, [filters]);

  useEffect(() => {
    const disasterId = searchParams.get('id');
    if (disasterId && disasters.length > 0) {
      const disaster = disasters.find(d => d.id === disasterId);
      if (disaster) {
        setSelectedDisaster(disaster);
      }
    }
  }, [searchParams, disasters]);

  const loadDisasters = async () => {
    try {
      setLoading(true);
      const params = {
        type: filters.type !== 'all' ? filters.type : undefined,
        severity: filters.severity !== 'all' ? filters.severity : undefined,
        days: parseInt(filters.dateRange),
      };
      const response = await disasterAPI.getDisasterMap(params);
      setDisasters(response.data.disasters || []);
    } catch (error) {
      toast.error('Failed to load disasters');
    } finally {
      setLoading(false);
    }
  };

  const handleDisasterClick = (disaster) => {
    setSelectedDisaster(disaster);
  };

  const handleExport = () => {
    const dataStr = JSON.stringify(disasters, null, 2);
    const dataBlob = new Blob([dataStr], { type: 'application/json' });
    const url = URL.createObjectURL(dataBlob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `disasters-${new Date().toISOString()}.json`;
    link.click();
    URL.revokeObjectURL(url);
    toast.success('Data exported successfully');
  };

  return (
    <div className="h-full flex flex-col">
      {/* Header */}
      <div className="bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 p-4">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold">3D Disaster Map</h1>
            <p className="text-sm text-gray-600 dark:text-gray-400">
              {disasters.length} disasters shown
            </p>
          </div>
          <div className="flex gap-2">
            <button
              onClick={() => setShowFilters(!showFilters)}
              className="btn btn-secondary"
            >
              <FaFilter className="mr-2" />
              Filters
            </button>
            <button onClick={handleExport} className="btn btn-secondary">
              <FaDownload className="mr-2" />
              Export
            </button>
          </div>
        </div>

        {/* Filters Panel */}
        {showFilters && (
          <div className="mt-4 p-4 bg-gray-50 dark:bg-gray-900 rounded-lg">
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <label className="block text-sm font-medium mb-2">
                  Disaster Type
                </label>
                <select
                  value={filters.type}
                  onChange={(e) => setFilters({ ...filters, type: e.target.value })}
                  className="input"
                >
                  <option value="all">All Types</option>
                  <option value="earthquake">Earthquake</option>
                  <option value="flood">Flood</option>
                  <option value="hurricane">Hurricane</option>
                  <option value="wildfire">Wildfire</option>
                  <option value="tornado">Tornado</option>
                  <option value="tsunami">Tsunami</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium mb-2">
                  Severity
                </label>
                <select
                  value={filters.severity}
                  onChange={(e) => setFilters({ ...filters, severity: e.target.value })}
                  className="input"
                >
                  <option value="all">All Severities</option>
                  <option value="low">Low</option>
                  <option value="medium">Medium</option>
                  <option value="high">High</option>
                  <option value="critical">Critical</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium mb-2">
                  Date Range
                </label>
                <select
                  value={filters.dateRange}
                  onChange={(e) => setFilters({ ...filters, dateRange: e.target.value })}
                  className="input"
                >
                  <option value="7">Last 7 days</option>
                  <option value="30">Last 30 days</option>
                  <option value="90">Last 90 days</option>
                  <option value="365">Last year</option>
                </select>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* 3D Viewer */}
      <div className="flex-1 relative">
        {loading ? (
          <div className="flex items-center justify-center h-full">
            <div className="spinner"></div>
          </div>
        ) : (
          <DisasterViewer3D
            disasters={disasters}
            selectedDisaster={selectedDisaster}
            onDisasterClick={handleDisasterClick}
          />
        )}
      </div>

      {/* Details Panel */}
      {selectedDisaster && (
        <div className="absolute right-4 top-24 w-80 max-h-[calc(100vh-200px)] overflow-y-auto bg-white dark:bg-gray-800 rounded-lg shadow-xl p-4 animate-slide-in-right">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-bold">Disaster Details</h3>
            <button
              onClick={() => setSelectedDisaster(null)}
              className="text-gray-500 hover:text-gray-700 dark:hover:text-gray-300"
            >
              ✕
            </button>
          </div>
          <div className="space-y-3">
            <div>
              <label className="text-sm text-gray-600 dark:text-gray-400">Name</label>
              <p className="font-medium">{selectedDisaster.name}</p>
            </div>
            <div>
              <label className="text-sm text-gray-600 dark:text-gray-400">Type</label>
              <p className="font-medium capitalize">{selectedDisaster.type}</p>
            </div>
            <div>
              <label className="text-sm text-gray-600 dark:text-gray-400">Severity</label>
              <span className={`badge ${
                selectedDisaster.severity === 'critical' ? 'badge-danger' :
                selectedDisaster.severity === 'high' ? 'badge-warning' :
                'badge-success'
              }`}>
                {selectedDisaster.severity}
              </span>
            </div>
            <div>
              <label className="text-sm text-gray-600 dark:text-gray-400">Location</label>
              <p className="font-medium">{selectedDisaster.location}</p>
            </div>
            <div>
              <label className="text-sm text-gray-600 dark:text-gray-400">Coordinates</label>
              <p className="text-sm font-mono">
                {selectedDisaster.latitude?.toFixed(4)}, {selectedDisaster.longitude?.toFixed(4)}
              </p>
            </div>
            {selectedDisaster.description && (
              <div>
                <label className="text-sm text-gray-600 dark:text-gray-400">Description</label>
                <p className="text-sm">{selectedDisaster.description}</p>
              </div>
            )}
            <div>
              <label className="text-sm text-gray-600 dark:text-gray-400">Date</label>
              <p className="text-sm">
                {new Date(selectedDisaster.created_at).toLocaleString()}
              </p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default DisasterMap;
