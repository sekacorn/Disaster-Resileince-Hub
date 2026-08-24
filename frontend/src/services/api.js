import axios from 'axios';
import { toast } from 'react-toastify';
import { getAccessToken, clearAccessToken } from './tokenStorage';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8000';
const DEMO_MODE = import.meta.env.VITE_DEMO_MODE === 'true';

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

const demoDisasters = [
  {
    id: 'wildfire-ca-001',
    name: 'Sierra Ridge Wildfire',
    type: 'wildfire',
    severity: 'critical',
    location: 'El Dorado County, CA',
    latitude: 38.7426,
    longitude: -120.4358,
    affected_population: 18400,
    casualties: 0,
    description: 'Fast-moving wildfire threatening ridge communities and power infrastructure.',
    created_at: '2026-08-21T08:15:00Z',
  },
  {
    id: 'flood-tx-014',
    name: 'Trinity River Flood Watch',
    type: 'flood',
    severity: 'high',
    location: 'Dallas County, TX',
    latitude: 32.7767,
    longitude: -96.7970,
    affected_population: 9200,
    casualties: 0,
    description: 'River levels are rising after heavy rainfall across upstream watersheds.',
    created_at: '2026-08-20T22:40:00Z',
  },
  {
    id: 'hurricane-fl-003',
    name: 'Coastal Surge Advisory',
    type: 'hurricane',
    severity: 'medium',
    location: 'Tampa Bay, FL',
    latitude: 27.9506,
    longitude: -82.4572,
    affected_population: 31600,
    casualties: 0,
    description: 'Storm surge planning advisory for low-lying neighborhoods and hospitals.',
    created_at: '2026-08-19T17:20:00Z',
  },
  {
    id: 'earthquake-ak-009',
    name: 'Cook Inlet Seismic Event',
    type: 'earthquake',
    severity: 'medium',
    location: 'Anchorage, AK',
    latitude: 61.2181,
    longitude: -149.9003,
    affected_population: 4100,
    casualties: 0,
    description: 'Moderate quake with bridge and utility inspection teams dispatched.',
    created_at: '2026-08-18T13:05:00Z',
  },
];

let demoRoutes = [
  {
    id: 'route-101',
    name: 'North Ridge Shelter Route',
    distance: 18.7,
    duration: 34,
    safety_score: 88,
    waypoints: [
      { name: 'Pine Valley High School', latitude: 38.732, longitude: -120.446, instructions: 'Start at the east parking exit.' },
      { name: 'County Road 11 Checkpoint', latitude: 38.759, longitude: -120.389, instructions: 'Stay in the marked evacuation lane.' },
      { name: 'North Ridge Community Shelter', latitude: 38.813, longitude: -120.335, instructions: 'Arrive at the south intake gate.' },
    ],
  },
  {
    id: 'route-204',
    name: 'Hospital Transfer Corridor',
    distance: 9.4,
    duration: 21,
    safety_score: 81,
    waypoints: [
      { name: 'Mercy Regional Hospital', latitude: 32.781, longitude: -96.809, instructions: 'Use ambulance staging bay.' },
      { name: 'I-35 Managed Lane', latitude: 32.803, longitude: -96.821, instructions: 'Avoid flooded service roads.' },
      { name: 'North Field Clinic', latitude: 32.855, longitude: -96.844, instructions: 'Unload at triage entrance.' },
    ],
  },
];

let demoRooms = [
  { id: 'ops-room', name: 'Emergency Operations', active_users: 5 },
  { id: 'shelter-room', name: 'Shelter Coordination', active_users: 3 },
  { id: 'medical-room', name: 'Medical Logistics', active_users: 4 },
];

const demoRoomDetails = {
  'ops-room': {
    active_users: [
      demoUser,
      { id: 'u2', full_name: 'Maya Chen', email: 'maya@example.com', role: 'incident commander' },
      { id: 'u3', full_name: 'Luis Ortega', email: 'luis@example.com', role: 'field lead' },
    ],
    messages: [
      { user_id: 'u2', user_name: 'Maya Chen', content: 'Wind shift expected at 1500. Keep evacuation routes northbound.', timestamp: '2026-08-21T13:32:00Z' },
      { user_id: 'local-demo-user', user_name: 'Demo User', content: 'Dashboard updated with the newest shelter capacity numbers.', timestamp: '2026-08-21T13:36:00Z' },
      { user_id: 'u3', user_name: 'Luis Ortega', content: 'Road crews cleared debris on County Road 11.', timestamp: '2026-08-21T13:41:00Z' },
    ],
  },
  'shelter-room': {
    active_users: [demoUser, { id: 'u4', full_name: 'Priya Shah', email: 'priya@example.com', role: 'shelter manager' }],
    messages: [
      { user_id: 'u4', user_name: 'Priya Shah', content: 'North Ridge has 420 available beds and generator fuel through tomorrow.', timestamp: '2026-08-21T12:50:00Z' },
    ],
  },
  'medical-room': {
    active_users: [demoUser, { id: 'u5', full_name: 'Sam Rivera', email: 'sam@example.com', role: 'medical logistics' }],
    messages: [
      { user_id: 'u5', user_name: 'Sam Rivera', content: 'Two mobile clinics are ready for deployment along the transfer corridor.', timestamp: '2026-08-21T12:18:00Z' },
    ],
  },
};

const mock = (data) => Promise.resolve({ data });

const api = axios.create({
  baseURL: `${API_URL}/api/v1`,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to add JWT token
api.interceptors.request.use(
  (config) => {
    const token = getAccessToken();
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
          clearAccessToken();
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
  login: (email, password) => DEMO_MODE
    ? mock({ access_token: 'demo-access-token', user: { ...demoUser, email } })
    : api.post('/auth/login', { email, password }),
  register: (userData) => DEMO_MODE ? mock({ ...demoUser, ...userData }) : api.post('/auth/register', userData),
  logout: () => DEMO_MODE ? mock({ message: 'Logged out successfully' }) : api.post('/auth/logout'),
  getMe: () => DEMO_MODE ? mock(demoUser) : api.get('/auth/me'),
  setupMFA: () => DEMO_MODE ? mock({ qrCodeDataUri: '', backupCodes: ['18374620', '92837465'] }) : api.post('/auth/mfa/setup'),
  verifyMFA: (code) => DEMO_MODE ? mock({ verified: true, code }) : api.post('/auth/mfa/verify', { code }),
  disableMFA: () => DEMO_MODE ? mock({ mfa_enabled: false }) : api.post('/auth/mfa/disable'),
};

export const dataAPI = {
  uploadData: (formData) => DEMO_MODE ? mock({ id: `dataset-${Date.now()}`, status: 'processed' }) : api.post('/data/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }),
  listDatasets: (params) => DEMO_MODE ? mock({ datasets: [] }) : api.get('/data/datasets', { params }),
  getDataset: (id) => DEMO_MODE ? mock({ id }) : api.get(`/data/datasets/${id}`),
  deleteDataset: (id) => DEMO_MODE ? mock({ deleted: true, id }) : api.delete(`/data/datasets/${id}`),
  validateData: (id) => DEMO_MODE ? mock({ valid: true, id }) : api.post(`/data/datasets/${id}/validate`),
};

export const disasterAPI = {
  listDisasters: (params) => DEMO_MODE ? mock({ disasters: demoDisasters }) : api.get('/disasters', { params }),
  getDisaster: (id) => DEMO_MODE ? mock(demoDisasters.find((disaster) => disaster.id === id)) : api.get(`/disasters/${id}`),
  createDisaster: (data) => DEMO_MODE ? mock({ id: `disaster-${Date.now()}`, ...data }) : api.post('/disasters', data),
  updateDisaster: (id, data) => DEMO_MODE ? mock({ id, ...data }) : api.put(`/disasters/${id}`, data),
  getDisasterStats: () => DEMO_MODE
    ? mock({ active_disasters: 4, total_evacuations: 17, active_users: 42, total_data_points: 128640 })
    : api.get('/disasters/stats'),
  getDisasterMap: (params) => DEMO_MODE
    ? mock({
        disasters: demoDisasters.filter((disaster) => {
          const typeMatches = !params?.type || disaster.type === params.type;
          const severityMatches = !params?.severity || disaster.severity === params.severity;
          return typeMatches && severityMatches;
        }),
      })
    : api.get('/disasters/map', { params }),
};

export const evacuationAPI = {
  planRoute: (data) => {
    if (!DEMO_MODE) return api.post('/evacuation/plan', data);
    const route = {
      id: `route-${Date.now()}`,
      name: `${data.start_location.name} to ${data.end_location.name}`,
      distance: 14.2,
      duration: data.vehicle_type === 'walking' ? 126 : 28,
      safety_score: 84,
      waypoints: [
        { ...data.start_location, instructions: 'Begin evacuation from the safest marked exit.' },
        { name: 'Traffic Control Point', latitude: (data.start_location.latitude + data.end_location.latitude) / 2, longitude: (data.start_location.longitude + data.end_location.longitude) / 2, instructions: 'Follow responder instructions at checkpoint.' },
        { ...data.end_location, instructions: 'Check in with shelter intake staff.' },
      ],
    };
    demoRoutes = [route, ...demoRoutes];
    return mock(route);
  },
  getRoutes: (params) => DEMO_MODE ? mock({ routes: demoRoutes }) : api.get('/evacuation/routes', { params }),
  getRoute: (id) => DEMO_MODE ? mock(demoRoutes.find((route) => route.id === id)) : api.get(`/evacuation/routes/${id}`),
  optimizeRoute: (id) => DEMO_MODE
    ? mock({ ...demoRoutes.find((route) => route.id === id), duration: 19, safety_score: 93 })
    : api.post(`/evacuation/routes/${id}/optimize`),
  getShelters: (params) => DEMO_MODE ? mock({ shelters: [] }) : api.get('/evacuation/shelters', { params }),
};

export const llmAPI = {
  chat: (messages, context) => DEMO_MODE
    ? mock({ message: 'Mock analysis: prioritize northbound evacuation, confirm shelter generator capacity, and stage medical transport near the traffic control point.' })
    : api.post('/llm/chat', { messages, context }),
  getRecommendations: (disasterId) => DEMO_MODE
    ? mock({ recommendations: ['Open overflow shelter capacity', 'Pre-stage water and medical supplies', 'Issue multilingual SMS alerts'] })
    : api.get(`/llm/recommendations/${disasterId}`),
  analyzeRisk: (data) => DEMO_MODE ? mock({ risk: 'high', confidence: 0.87, factors: ['wind', 'population density'] }) : api.post('/llm/analyze-risk', data),
};

export const collaborationAPI = {
  getRooms: () => DEMO_MODE ? mock({ rooms: demoRooms }) : api.get('/collaboration/rooms'),
  getRoom: (id) => DEMO_MODE ? mock(demoRoomDetails[id] || { active_users: [demoUser], messages: [] }) : api.get(`/collaboration/rooms/${id}`),
  createRoom: (data) => {
    if (!DEMO_MODE) return api.post('/collaboration/rooms', data);
    const room = { id: `room-${Date.now()}`, name: data.name, active_users: 1 };
    demoRooms = [room, ...demoRooms];
    demoRoomDetails[room.id] = { active_users: [demoUser], messages: [] };
    return mock(room);
  },
  joinRoom: (id) => DEMO_MODE ? mock({ joined: true, id }) : api.post(`/collaboration/rooms/${id}/join`),
  leaveRoom: (id) => DEMO_MODE ? mock({ left: true, id }) : api.post(`/collaboration/rooms/${id}/leave`),
  sendMessage: (roomId, message) => {
    if (!DEMO_MODE) return api.post(`/collaboration/rooms/${roomId}/messages`, message);
    demoRoomDetails[roomId]?.messages.push(message);
    return mock(message);
  },
};

export const userAPI = {
  getProfile: () => DEMO_MODE ? mock(demoUser) : api.get('/users/profile'),
  updateProfile: (data) => DEMO_MODE ? mock({ ...demoUser, ...data }) : api.put('/users/profile', data),
  updatePreferences: (data) => DEMO_MODE ? mock(data) : api.put('/users/preferences', data),
  getActivityLog: () => DEMO_MODE
    ? mock({
        activities: [
          { action: 'Reviewed route', description: 'Opened North Ridge Shelter Route details', timestamp: '2026-08-21T13:44:00Z' },
          { action: 'Exported map data', description: 'Downloaded active disaster GeoJSON snapshot', timestamp: '2026-08-21T12:58:00Z' },
          { action: 'Joined room', description: 'Joined Emergency Operations collaboration room', timestamp: '2026-08-21T12:12:00Z' },
        ],
      })
    : api.get('/users/activity-log'),
};

export default api;
