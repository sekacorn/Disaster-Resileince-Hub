import { Link, useLocation } from 'react-router-dom';
import {
  FaHome,
  FaMap,
  FaRoute,
  FaUsers,
  FaUpload,
  FaChartBar,
  FaComments,
} from 'react-icons/fa';

const Sidebar = () => {
  const location = useLocation();

  const menuItems = [
    { path: '/dashboard', label: 'Dashboard', icon: <FaHome /> },
    { path: '/disaster-map', label: 'Disaster Map', icon: <FaMap /> },
    { path: '/evacuation-planner', label: 'Evacuation', icon: <FaRoute /> },
    { path: '/collaborate', label: 'Collaborate', icon: <FaUsers /> },
    { path: '/upload', label: 'Upload Data', icon: <FaUpload /> },
    { path: '/analytics', label: 'Analytics', icon: <FaChartBar /> },
    { path: '/chat', label: 'AI Assistant', icon: <FaComments /> },
  ];

  const isActive = (path) => location.pathname === path;

  return (
    <aside className="w-64 bg-white dark:bg-gray-800 border-r border-gray-200 dark:border-gray-700 overflow-y-auto">
      {/*
        Named so it is distinguishable from the other navigation landmarks on the
        page. An unnamed <nav> is announced only as "navigation", which is useless
        when a page has several (WCAG 1.3.1, 2.4.1).
      */}
      <nav className="p-4" aria-label="Main">
        <ul className="space-y-2">
          {menuItems.map((item) => {
            const active = isActive(item.path);
            return (
              <li key={item.path}>
                <Link
                  to={item.path}
                  /*
                   * Marks the current page programmatically. Previously the active
                   * item was distinguished by background colour alone, which 1.4.1
                   * Use of Colour does not permit and which no screen reader conveys.
                   */
                  aria-current={active ? 'page' : undefined}
                  className={`flex items-center space-x-3 px-4 py-3 rounded-lg transition-colors ${
                    active
                      ? 'bg-primary-100 dark:bg-primary-900 text-primary-800 dark:text-primary-200 font-medium'
                      : 'hover:bg-gray-100 dark:hover:bg-gray-700'
                  }`}
                >
                  {/* Icons duplicate the adjacent label, so they are decorative. */}
                  <span className="text-xl" aria-hidden="true">
                    {item.icon}
                  </span>
                  <span>{item.label}</span>
                </Link>
              </li>
            );
          })}
        </ul>
      </nav>
    </aside>
  );
};

export default Sidebar;
