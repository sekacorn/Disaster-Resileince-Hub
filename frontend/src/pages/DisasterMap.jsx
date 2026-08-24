import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import DisasterViewer3D from '@components/visualization/DisasterViewer3D';
import DisasterDataTable from '@components/visualization/DisasterDataTable';
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
                <label htmlFor="filter-type" className="block text-sm font-medium mb-2">
                  Disaster Type
                </label>
                <select
                  id="filter-type"
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
                <label htmlFor="filter-severity" className="block text-sm font-medium mb-2">
                  Severity
                </label>
                <select
                  id="filter-severity"
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
                <label htmlFor="filter-date-range" className="block text-sm font-medium mb-2">
                  Date Range
                </label>
                <select
                  id="filter-date-range"
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
          <div className="flex items-center justify-center h-full" role="status" aria-live="polite">
            <div className="spinner" aria-hidden="true"></div>
            <span className="sr-only">Loading disaster data</span>
          </div>
        ) : (
          <DisasterViewer3D
            disasters={disasters}
            selectedDisaster={selectedDisaster}
            onDisasterClick={handleDisasterClick}
          />
        )}
      </div>

      {/*
        The accessible equivalent of the 3D scene above. Rendered for everyone, not
        gated behind a "accessible version" toggle: a separate-but-equal page is
        harder to keep in sync and signals that access is an afterthought.
      */}
      {!loading && (
        <div className="p-4">
          <DisasterDataTable
            disasters={disasters}
            selectedDisaster={selectedDisaster}
            onDisasterClick={handleDisasterClick}
          />
        </div>
      )}

      {/* Details Panel */}
      {selectedDisaster && (
        <section
          className="absolute right-4 top-24 w-80 max-h-[calc(100vh-200px)] overflow-y-auto bg-white dark:bg-gray-800 rounded-lg shadow-xl p-4 animate-slide-in-right"
          /* <section> with an accessible name is already a region landmark. */
          aria-labelledby="disaster-details-heading"
          /* Appears in response to a user action, so announce it (WCAG 4.1.3). */
          aria-live="polite"
        >
          <div className="flex items-center justify-between mb-4">
            <h3 id="disaster-details-heading" className="text-lg font-bold">
              Disaster Details
            </h3>
            <button
              type="button"
              onClick={() => setSelectedDisaster(null)}
              className="text-gray-600 hover:text-gray-800 dark:text-gray-400 dark:hover:text-gray-200"
              /* The glyph is announced as "multiplication x" without this. */
              aria-label="Close disaster details"
            >
              <span aria-hidden="true">✕</span>
            </button>
          </div>
          {/*
            Was a list of <label> elements with nothing to label, which is invalid and
            announces as an orphaned form control. A description list is the correct
            structure for name/value pairs (WCAG 1.3.1).
          */}
          <dl className="space-y-3">
            <div>
              <dt className="text-sm text-gray-600 dark:text-gray-400">Name</dt>
              <dd><p className="font-medium">{selectedDisaster.name}</p></dd>
            </div>
            <div>
              <dt className="text-sm text-gray-600 dark:text-gray-400">Type</dt>
              <dd><p className="font-medium capitalize">{selectedDisaster.type}</p></dd>
            </div>
            <div>
              <dt className="text-sm text-gray-600 dark:text-gray-400">Severity</dt>
              <dd>
              <span className={`badge ${
                selectedDisaster.severity === 'critical' ? 'badge-danger' :
                selectedDisaster.severity === 'high' ? 'badge-warning' :
                'badge-success'
              }`}>
                {selectedDisaster.severity}
              </span>
              </dd>
            </div>
            <div>
              <dt className="text-sm text-gray-600 dark:text-gray-400">Location</dt>
              <dd><p className="font-medium">{selectedDisaster.location}</p></dd>
            </div>
            <div>
              <dt className="text-sm text-gray-600 dark:text-gray-400">Coordinates</dt>
              <dd className="text-sm font-mono">
                {selectedDisaster.latitude?.toFixed(4)}, {selectedDisaster.longitude?.toFixed(4)}
              </dd>
            </div>
            {selectedDisaster.description && (
              <div>
                <dt className="text-sm text-gray-600 dark:text-gray-400">Description</dt>
                <dd className="text-sm">{selectedDisaster.description}</dd>
              </div>
            )}
            <div>
              <dt className="text-sm text-gray-600 dark:text-gray-400">Date</dt>
              <dd className="text-sm">
                {new Date(selectedDisaster.created_at).toLocaleString()}
              </dd>
            </div>
          </dl>
        </section>
      )}
    </div>
  );
};

export default DisasterMap;
