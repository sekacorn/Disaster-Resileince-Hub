import { Link } from 'react-router-dom';
import { FaShieldAlt, FaMap, FaUsers, FaChartLine, FaBrain, FaRoute } from 'react-icons/fa';

const Home = () => {
  const features = [
    {
      icon: <FaMap className="w-8 h-8" />,
      title: '3D Disaster Mapping',
      description: 'Visualize disasters in real-time with interactive 3D maps powered by Three.js',
    },
    {
      icon: <FaRoute className="w-8 h-8" />,
      title: 'Smart Evacuation Planning',
      description: 'AI-powered route optimization for safe and efficient evacuations',
    },
    {
      icon: <FaBrain className="w-8 h-8" />,
      title: 'LLM-Powered Insights',
      description: 'Get intelligent recommendations and analysis from advanced AI models',
    },
    {
      icon: <FaUsers className="w-8 h-8" />,
      title: 'Real-time Collaboration',
      description: 'Work together with teams through WebSocket-enabled collaboration tools',
    },
    {
      icon: <FaChartLine className="w-8 h-8" />,
      title: 'Data Analytics',
      description: 'Advanced analytics and visualization for disaster preparedness',
    },
    {
      icon: <FaShieldAltAlt className="w-8 h-8" />,
      title: 'MBTI-Tailored Experience',
      description: 'Personalized UI and workflows based on your personality type',
    },
  ];

  return (
    <div className="min-h-screen">
      {/* Hero Section */}
      <section className="relative bg-gradient-to-br from-primary-600 via-primary-700 to-primary-900 text-white py-20 px-4">
        <div className="max-w-7xl mx-auto text-center">
          <h1 className="text-5xl md:text-6xl font-bold mb-6">
            Disaster Resilience Hub
          </h1>
          <p className="text-xl md:text-2xl mb-8 text-primary-100">
            AI-Powered Disaster Management for a Safer Tomorrow
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <Link to="/register" className="btn btn-primary bg-white text-primary-700 hover:bg-gray-100 text-lg px-8 py-3">
              Get Started
            </Link>
            <Link to="/login" className="btn btn-secondary bg-primary-800 hover:bg-primary-900 text-white text-lg px-8 py-3">
              Sign In
            </Link>
          </div>
        </div>
        <div className="absolute inset-0 bg-gradient-to-t from-black/20 to-transparent pointer-events-none"></div>
      </section>

      {/* Features Section */}
      <section className="py-16 px-4 bg-white dark:bg-gray-900">
        <div className="max-w-7xl mx-auto">
          <h2 className="text-4xl font-bold text-center mb-12">
            Comprehensive Disaster Management Platform
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
            {features.map((feature, index) => (
              <div
                key={index}
                className="card hover:shadow-xl transition-shadow duration-300 border border-gray-200 dark:border-gray-700"
              >
                <div className="text-primary-600 dark:text-primary-400 mb-4">
                  {feature.icon}
                </div>
                <h3 className="text-xl font-bold mb-2">{feature.title}</h3>
                <p className="text-gray-600 dark:text-gray-400">
                  {feature.description}
                </p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Stats Section */}
      <section className="py-16 px-4 bg-gray-50 dark:bg-gray-800">
        <div className="max-w-7xl mx-auto">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-8 text-center">
            <div>
              <div className="text-4xl font-bold text-primary-600 dark:text-primary-400 mb-2">
                24/7
              </div>
              <div className="text-gray-600 dark:text-gray-400">
                Real-time Monitoring
              </div>
            </div>
            <div>
              <div className="text-4xl font-bold text-primary-600 dark:text-primary-400 mb-2">
                AI-Powered
              </div>
              <div className="text-gray-600 dark:text-gray-400">
                Smart Recommendations
              </div>
            </div>
            <div>
              <div className="text-4xl font-bold text-primary-600 dark:text-primary-400 mb-2">
                Multi-User
              </div>
              <div className="text-gray-600 dark:text-gray-400">
                Collaboration Tools
              </div>
            </div>
            <div>
              <div className="text-4xl font-bold text-primary-600 dark:text-primary-400 mb-2">
                16 Types
              </div>
              <div className="text-gray-600 dark:text-gray-400">
                MBTI Personalization
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-16 px-4 bg-primary-600 text-white">
        <div className="max-w-4xl mx-auto text-center">
          <h2 className="text-3xl md:text-4xl font-bold mb-4">
            Ready to Build Resilience?
          </h2>
          <p className="text-xl mb-8 text-primary-100">
            Join disaster response teams worldwide using our platform
          </p>
          <Link
            to="/register"
            className="btn bg-white text-primary-700 hover:bg-gray-100 text-lg px-8 py-3"
          >
            Start Your Free Account
          </Link>
        </div>
      </section>
    </div>
  );
};

export default Home;
