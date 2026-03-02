import axios from 'axios';
import { toast } from 'react-toastify';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8000';

const api = axios.create({
  baseURL: `${API_URL}/api/v1`,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to add JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor to handle errors globally
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      switch (error.response.status) {
        case 401:
          // Unauthorized - token expired or invalid
          localStorage.removeItem('token');
          window.location.href = '/login';
          toast.error('Session expired. Please login again.');
          break;
        case 403:
          toast.error('Access denied');
          break;
        case 404:
          toast.error('Resource not found');
          break;
        case 500:
          toast.error('Server error. Please try again later.');
          break;
        default:
          // Handle other errors
          break;
      }
    } else if (error.request) {
      toast.error('Network error. Please check your connection.');
    }
    return Promise.reject(error);
  }
);

// API methods
export const authAPI = {
  login: (email, password) => api.post('/auth/login', { email, password }),
  register: (userData) => api.post('/auth/register', userData),
  logout: () => api.post('/auth/logout'),
  getMe: () => api.get('/auth/me'),
  setupMFA: () => api.post('/auth/mfa/setup'),
  verifyMFA: (code) => api.post('/auth/mfa/verify', { code }),
  disableMFA: () => api.post('/auth/mfa/disable'),
};

export const dataAPI = {
  uploadData: (formData) => api.post('/data/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }),
  listDatasets: (params) => api.get('/data/datasets', { params }),
  getDataset: (id) => api.get(`/data/datasets/${id}`),
  deleteDataset: (id) => api.delete(`/data/datasets/${id}`),
  validateData: (id) => api.post(`/data/datasets/${id}/validate`),
};

export const disasterAPI = {
  listDisasters: (params) => api.get('/disasters', { params }),
  getDisaster: (id) => api.get(`/disasters/${id}`),
  createDisaster: (data) => api.post('/disasters', data),
  updateDisaster: (id, data) => api.put(`/disasters/${id}`, data),
  getDisasterStats: () => api.get('/disasters/stats'),
  getDisasterMap: (params) => api.get('/disasters/map', { params }),
};

export const evacuationAPI = {
  planRoute: (data) => api.post('/evacuation/plan', data),
  getRoutes: (params) => api.get('/evacuation/routes', { params }),
  getRoute: (id) => api.get(`/evacuation/routes/${id}`),
  optimizeRoute: (id) => api.post(`/evacuation/routes/${id}/optimize`),
  getShelters: (params) => api.get('/evacuation/shelters', { params }),
};

export const llmAPI = {
  chat: (messages, context) => api.post('/llm/chat', { messages, context }),
  getRecommendations: (disasterId) => api.get(`/llm/recommendations/${disasterId}`),
  analyzeRisk: (data) => api.post('/llm/analyze-risk', data),
};

export const collaborationAPI = {
  getRooms: () => api.get('/collaboration/rooms'),
  getRoom: (id) => api.get(`/collaboration/rooms/${id}`),
  createRoom: (data) => api.post('/collaboration/rooms', data),
  joinRoom: (id) => api.post(`/collaboration/rooms/${id}/join`),
  leaveRoom: (id) => api.post(`/collaboration/rooms/${id}/leave`),
  sendMessage: (roomId, message) => api.post(`/collaboration/rooms/${roomId}/messages`, message),
};

export const userAPI = {
  getProfile: () => api.get('/users/profile'),
  updateProfile: (data) => api.put('/users/profile', data),
  updatePreferences: (data) => api.put('/users/preferences', data),
  getActivityLog: () => api.get('/users/activity-log'),
};

export default api;
