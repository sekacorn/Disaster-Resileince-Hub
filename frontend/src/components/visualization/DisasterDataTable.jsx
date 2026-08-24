/**
 * Keyboard-operable equivalent of the 3D disaster scene.
 *
 * A WebGL canvas paints pixels. It exposes no accessibility tree, its markers cannot
 * be reached by Tab, and orbiting the camera requires a drag. That makes the 3D view
 * alone a failure of 1.1.1 Non-text Content, 2.1.1 Keyboard and 1.4.1 Use of Colour,
 * where severity is carried by marker colour.
 *
 * Rather than trying to make the canvas itself accessible -- which WebGL does not
 * really allow -- this presents the same underlying data as a table: the equivalent
 * that 1.1.1 asks for. It is not a fallback shown only to screen readers; it is
 * visible to everyone, because a sortable table is often the faster way to read
 * incident data even for someone who can see the 3D view.
 *
 * @param {Array} disasters            same array the scene renders
 * @param {object} [selectedDisaster]  currently selected incident, if any
 * @param {Function} onDisasterClick   selection handler shared with the scene
 */
const SEVERITY_DESCRIPTIONS = {
  CRITICAL: 'Critical severity',
  HIGH: 'High severity',
  MEDIUM: 'Medium severity',
  LOW: 'Low severity',
};

const SEVERITY_BADGE_CLASS = {
  CRITICAL: 'badge-danger',
  HIGH: 'badge-warning',
  MEDIUM: 'badge-warning',
  LOW: 'badge-success',
};

const formatCoordinate = (value) =>
  typeof value === 'number' ? value.toFixed(4) : 'Not recorded';

const DisasterDataTable = ({ disasters = [], selectedDisaster, onDisasterClick }) => {
  if (disasters.length === 0) {
    return (
      <div className="card" role="status">
        <h2 className="text-lg font-bold mb-2">Active incidents</h2>
        <p className="text-gray-600 dark:text-gray-400">
          No active incidents are being tracked right now.
        </p>
      </div>
    );
  }

  return (
    <div className="card">
      <h2 id="disaster-table-heading" className="text-lg font-bold mb-1">
        Active incidents
      </h2>
      <p id="disaster-table-description" className="text-sm text-gray-600 dark:text-gray-400 mb-4">
        The same incidents shown on the 3D map, as a table. Selecting a row highlights
        that incident on the map.
      </p>

      {/*
        Horizontal scrolling is confined to the table. Letting the page itself scroll
        sideways breaks 1.4.10 Reflow at 320px.
      */}
      <div className="overflow-x-auto">
        <table
          className="w-full text-left border-collapse"
          aria-labelledby="disaster-table-heading"
          aria-describedby="disaster-table-description"
        >
          <caption className="sr-only">
            Active disaster incidents with type, severity, location and coordinates.
            {disasters.length} incidents listed.
          </caption>
          <thead>
            <tr className="border-b border-gray-200 dark:border-gray-700">
              {/* scope tells a screen reader which cells each header governs (1.3.1). */}
              <th scope="col" className="py-2 pr-4 font-semibold">
                Incident
              </th>
              <th scope="col" className="py-2 pr-4 font-semibold">
                Type
              </th>
              <th scope="col" className="py-2 pr-4 font-semibold">
                Severity
              </th>
              <th scope="col" className="py-2 pr-4 font-semibold">
                Location
              </th>
              <th scope="col" className="py-2 pr-4 font-semibold">
                Coordinates
              </th>
              <th scope="col" className="py-2 font-semibold">
                <span className="sr-only">Actions</span>
              </th>
            </tr>
          </thead>
          <tbody>
            {disasters.map((disaster, index) => {
              const id = disaster.id ?? index;
              const severity = (disaster.severity || 'LOW').toUpperCase();
              const isSelected = selectedDisaster?.id === disaster.id;
              const name = disaster.name || disaster.title || `Incident ${index + 1}`;

              return (
                <tr
                  key={id}
                  className={`border-b border-gray-100 dark:border-gray-800 ${
                    isSelected ? 'bg-primary-50 dark:bg-primary-900/30' : ''
                  }`}
                >
                  {/* Row header: identifies the row when navigating cell by cell. */}
                  <th scope="row" className="py-3 pr-4 font-medium">
                    {name}
                    {isSelected && (
                      // Selection is otherwise conveyed by background colour alone.
                      <span className="sr-only"> (currently selected)</span>
                    )}
                  </th>
                  <td className="py-3 pr-4">{disaster.type || 'Unspecified'}</td>
                  <td className="py-3 pr-4">
                    <span className={`badge ${SEVERITY_BADGE_CLASS[severity] || 'badge-primary'}`}>
                      {SEVERITY_DESCRIPTIONS[severity] || severity}
                    </span>
                  </td>
                  <td className="py-3 pr-4">
                    {disaster.location || disaster.city || 'Not recorded'}
                  </td>
                  <td className="py-3 pr-4 tabular-nums">
                    {formatCoordinate(disaster.latitude)}, {formatCoordinate(disaster.longitude)}
                  </td>
                  <td className="py-3">
                    <button
                      type="button"
                      onClick={() => onDisasterClick?.(disaster)}
                      className="btn btn-secondary text-sm"
                      // "View" repeated down a column is ambiguous out of context, and
                      // screen reader users routinely list buttons on their own (2.4.4).
                      aria-label={`View details for ${name}`}
                    >
                      View
                    </button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default DisasterDataTable;
