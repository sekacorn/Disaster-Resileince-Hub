import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { FaMap, FaRoute, FaUsers, FaChartBar, FaExclamationTriangle, FaUpload } from 'react-icons/fa';
import { useAuth } from '@hooks/useAuth';
import { disasterAPI } from '@services/api';
import { getMBTIStyle, getRecommendedFeatures } from '@utils/mbtiStyles';
import { toast } from 'react-toastify';

const Dashboard = () => {
  const { user } = useAuth();
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [recentDisasters, setRecentDisasters] = useState([]);

  const mbtiProfile = user?.mbti_type ? getMBTIStyle(user.mbti_type) : null;
  const recommendedFeatures = user?.mbti_type ? getRecommendedFeatures(user.mbti_type) : [];

  useEffect(() => {
    loadDashboardData();
  }, []);

  const loadDashboardData = async () => {
    try {
      setLoading(true);
      const [statsResponse, disastersResponse] = await Promise.all([
        disasterAPI.getDisasterStats(),
        disasterAPI.listDisasters({ limit: 5 }),
      ]);
      setStats(statsResponse.data);
      setRecentDisasters(disastersResponse.data.disasters || []);
    } catch (error) {
      toast.error('Failed to load dashboard data');
    } finally {
      setLoading(false);
    }
  };

  const quickActions = [
    {
      icon: <FaMap className="w-6 h-6" />,
      title: 'Disaster Map',
      description: 'View 3D disaster visualizations',
      link: '/disaster-map',
      color: 'bg-blue-500',
    },
    {
      icon: <FaRoute className="w-6 h-6" />,
      title: 'Evacuation Planner',
      description: 'Plan safe evacuation routes',
      link: '/evacuation-planner',
      color: 'bg-green-500',
    },
    {
      icon: <FaUsers className="w-6 h-6" />,
      title: 'Collaborate',
      description: 'Work with your team',
      link: '/collaborate',
      color: 'bg-purple-500',
    },
    {
      icon: <FaUpload className="w-6 h-6" />,
      title: 'Upload Data',
      description: 'Add disaster data',
      link: '/upload',
      color: 'bg-orange-500',
    },
  ];

  const statCards = [
    {
      label: 'Active Disasters',
      value: stats?.active_disasters || 0,
      icon: <FaExclamationTriangle />,
      color: 'text-danger-500',
      bgColor: 'bg-danger-50 dark:bg-danger-900/20',
    },
    {
      label: 'Evacuations Planned',
      value: stats?.total_evacuations || 0,
      icon: <FaRoute />,
      color: 'text-success-500',
      bgColor: 'bg-success-50 dark:bg-success-900/20',
    },
    {
      label: 'Active Users',
      value: stats?.active_users || 0,
      icon: <FaUsers />,
      color: 'text-primary-500',
      bgColor: 'bg-primary-50 dark:bg-primary-900/20',
    },
    {
      label: 'Data Points',
      value: stats?.total_data_points || 0,
      icon: <FaChartBar />,
      color: 'text-warning-500',
      bgColor: 'bg-warning-50 dark:bg-warning-900/20',
    },
  ];

  if (loading) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="spinner"></div>
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6">
      {/* Welcome Section */}
      <div className="card">
        <h1 className="text-3xl font-bold mb-2">
          Welcome back, {user?.full_name || user?.email}
        </h1>
        {mbtiProfile && (
          <p className="text-gray-600 dark:text-gray-400">
            Your profile: {mbtiProfile.name} ({user.mbti_type})
          </p>
        )}
        {user?.role && (
          <span className="badge badge-primary mt-2">
            {user.role.replace('_', ' ').toUpperCase()}
          </span>
        )}
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {statCards.map((stat, index) => (
          <div key={index} className="card">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-600 dark:text-gray-400 mb-1">
                  {stat.label}
                </p>
                <p className="text-3xl font-bold">{stat.value}</p>
              </div>
              <div className={`${stat.bgColor} ${stat.color} p-4 rounded-lg text-2xl`}>
                {stat.icon}
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Quick Actions */}
      <div>
        <h2 className="text-2xl font-bold mb-4">Quick Actions</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          {quickActions.map((action, index) => (
            <Link
              key={index}
              to={action.link}
              className="card hover:shadow-xl transition-shadow duration-300 border-2 border-transparent hover:border-primary-500"
            >
              <div className={`${action.color} text-white p-3 rounded-lg w-fit mb-3`}>
                {action.icon}
              </div>
              <h3 className="text-lg font-bold mb-1">{action.title}</h3>
              <p className="text-sm text-gray-600 dark:text-gray-400">
                {action.description}
              </p>
            </Link>
          ))}
        </div>
      </div>

      {/* Recommended Features (MBTI-based) */}
      {recommendedFeatures.length > 0 && (
        <div className="card bg-gradient-to-r from-primary-50 to-primary-100 dark:from-primary-900/20 dark:to-primary-800/20">
          <h3 className="text-lg font-bold mb-2">Recommended for You</h3>
          <p className="text-sm text-gray-600 dark:text-gray-400 mb-3">
            Based on your {user.mbti_type} personality type
          </p>
          <div className="flex flex-wrap gap-2">
            {recommendedFeatures.map((feature, index) => (
              <span key={index} className="badge badge-primary">
                {feature}
              </span>
            ))}
          </div>
        </div>
      )}

      {/* Recent Disasters */}
      <div>
        <h2 className="text-2xl font-bold mb-4">Recent Disasters</h2>
        {recentDisasters.length > 0 ? (
          <div className="space-y-3">
            {recentDisasters.map((disaster) => (
              <div key={disaster.id} className="card flex items-center justify-between">
                <div className="flex-1">
                  <h3 className="font-bold">{disaster.name}</h3>
                  <p className="text-sm text-gray-600 dark:text-gray-400">
                    {disaster.type} - {disaster.location}
                  </p>
                  <p className="text-xs text-gray-500 dark:text-gray-500 mt-1">
                    {new Date(disaster.created_at).toLocaleString()}
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <span className={`badge ${
                    disaster.severity === 'critical' ? 'badge-danger' :
                    disaster.severity === 'high' ? 'badge-warning' :
                    'badge-success'
                  }`}>
                    {disaster.severity}
                  </span>
                  <Link
                    to={`/disaster-map?id=${disaster.id}`}
                    className="btn btn-primary btn-sm"
                  >
                    View
                  </Link>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="card text-center text-gray-500">
            No recent disasters
          </div>
        )}
      </div>
    </div>
  );
};

export default Dashboard;
