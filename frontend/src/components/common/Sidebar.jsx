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
      <nav className="p-4 space-y-2">
        {menuItems.map((item) => (
          <Link
            key={item.path}
            to={item.path}
            className={`flex items-center space-x-3 px-4 py-3 rounded-lg transition-colors ${
              isActive(item.path)
                ? 'bg-primary-100 dark:bg-primary-900 text-primary-700 dark:text-primary-300 font-medium'
                : 'hover:bg-gray-100 dark:hover:bg-gray-700'
            }`}
          >
            <span className="text-xl">{item.icon}</span>
            <span>{item.label}</span>
          </Link>
        ))}
      </nav>
    </aside>
  );
};

export default Sidebar;
