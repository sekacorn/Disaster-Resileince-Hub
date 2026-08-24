import { createContext, useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '@services/api';
import { toast } from 'react-toastify';
import { getAccessToken, setAccessToken, clearAccessToken } from '@services/tokenStorage';

export const AuthContext = createContext(null);

const demoUser = {
  id: 'local-demo-user',
  username: 'demo',
  email: 'demo@example.com',
  firstName: 'Demo',
  lastName: 'User',
  full_name: 'Demo User',
  role: 'USER',
  organization: 'Local Development',
  mfa_enabled: false,
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    checkAuth();
  }, []);

  const checkAuth = async () => {
    const token = getAccessToken();
    if (token) {
      try {
        const response = await api.get('/auth/me');
        setUser(response.data);
      } catch (error) {
        clearAccessToken();
        setUser(import.meta.env.VITE_DEMO_MODE === 'true' ? demoUser : null);
      }
    } else if (import.meta.env.VITE_DEMO_MODE === 'true') {
      setUser(demoUser);
    }
    setLoading(false);
  };

  const login = async (email, password) => {
    try {
      const response = await api.post('/auth/login', { email, password });
      const { access_token, user: userData } = response.data;

      setAccessToken(access_token);
      setUser(userData);

      toast.success('Login successful!');
      navigate('/dashboard');

      return { success: true };
    } catch (error) {
      const message = error.response?.data?.detail || 'Login failed';
      toast.error(message);
      return { success: false, error: message };
    }
  };

  const register = async (userData) => {
    try {
      const response = await api.post('/auth/register', userData);
      toast.success('Registration successful! Please login.');
      navigate('/login');
      return { success: true };
    } catch (error) {
      const message = error.response?.data?.detail || 'Registration failed';
      toast.error(message);
      return { success: false, error: message };
    }
  };

  const logout = () => {
    clearAccessToken();
    setUser(null);
    toast.info('Logged out successfully');
    navigate('/');
  };

  const updateUser = (userData) => {
    setUser({ ...user, ...userData });
  };

  const value = {
    user,
    loading,
    isAuthenticated: !!user,
    login,
    register,
    logout,
    updateUser,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
