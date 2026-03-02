# Disaster Resilience Hub - Frontend

A modern React frontend for the Disaster Resilience Hub platform with Three.js 3D visualizations, real-time collaboration, and MBTI-tailored user experiences.

## Features

- **React 18** with Vite for fast development and optimized builds
- **Three.js 3D Visualizations** for interactive disaster mapping
- **Tailwind CSS** for responsive, customizable styling
- **Authentication** with JWT and optional MFA
- **MBTI-Tailored UI** - Personalized experiences based on personality types
- **Real-time Collaboration** via WebSockets
- **LLM Chat Interface** for AI-powered assistance
- **Evacuation Planning** with route optimization
- **Data Upload** with validation and processing
- **Dark Mode** support
- **Responsive Design** for all screen sizes

## Tech Stack

- **Framework:** React 18
- **Build Tool:** Vite
- **Styling:** Tailwind CSS
- **3D Graphics:** Three.js, React Three Fiber
- **State Management:** React Context API
- **Forms:** Formik + Yup
- **HTTP Client:** Axios
- **WebSockets:** Socket.IO Client
- **Routing:** React Router v6
- **Icons:** React Icons
- **Notifications:** React Toastify

## Project Structure

```
frontend/
├── src/
│   ├── components/
│   │   ├── auth/           # Authentication components
│   │   ├── collaboration/  # Real-time collaboration
│   │   ├── common/         # Shared components
│   │   ├── data/           # Data upload/management
│   │   ├── evacuation/     # Evacuation planning
│   │   ├── llm/            # AI chat interface
│   │   └── visualization/  # 3D disaster viewer
│   ├── contexts/           # React contexts
│   ├── hooks/              # Custom React hooks
│   ├── pages/              # Page components
│   ├── services/           # API and WebSocket services
│   ├── styles/             # Global styles
│   └── utils/              # Utility functions
├── public/                 # Static assets
├── Dockerfile             # Multi-stage Docker build
├── nginx.conf             # Nginx configuration
└── package.json           # Dependencies
```

## Getting Started

### Prerequisites

- Node.js 18+ and npm
- Backend API running (see backend README)

### Installation

1. Install dependencies:
```bash
npm install
```

2. Create environment file:
```bash
cp .env.example .env
```

3. Configure environment variables in `.env`:
```env
VITE_API_URL=http://localhost:8000
VITE_WS_URL=ws://localhost:8000
```

### Development

Start the development server:
```bash
npm run dev
```

The app will be available at `http://localhost:3000`

### Building for Production

Build the application:
```bash
npm run build
```

Preview the production build:
```bash
npm run preview
```

## Docker Deployment

### Build Docker Image

```bash
docker build -t disaster-hub-frontend .
```

### Run Container

```bash
docker run -p 80:80 \
  -e VITE_API_URL=http://api.example.com \
  -e VITE_WS_URL=ws://api.example.com \
  disaster-hub-frontend
```

### Docker Compose

The frontend is included in the main `docker-compose.yml` at the project root.

## Key Features

### 1. Authentication

- Email/password login with JWT tokens
- Registration with role selection
- Optional MFA (TOTP) setup
- Password strength validation
- Auto-refresh tokens

### 2. 3D Disaster Visualization

- Interactive Three.js scene
- Real-time disaster markers
- Color-coded severity levels
- Click for detailed information
- Orbital camera controls
- Responsive canvas rendering

### 3. MBTI-Tailored Experience

The UI adapts based on user's MBTI personality type:

- **INTJ/INTP**: Detailed analytics, high complexity
- **ENTJ/ENTP**: Executive dashboards, strategic views
- **INFJ/INFP**: Narrative-driven, empathetic displays
- **ENFJ/ENFP**: Collaborative, colorful interfaces
- **ISTJ/ISFJ**: Structured, organized layouts
- **ESTJ/ESFJ**: Professional, traditional dashboards
- **ISTP/ISFP**: Functional, visual-focused
- **ESTP/ESFP**: Action-oriented, interactive

### 4. Real-time Collaboration

- WebSocket-powered chat rooms
- Live user presence indicators
- Typing indicators
- Message history
- User avatars and roles

### 5. Evacuation Planning

- Interactive route planning
- Real-time route visualization
- Safety score calculation
- Route optimization
- Waypoint management
- Multiple vehicle types support

### 6. AI Chat Interface

- LLM-powered assistance
- Context-aware responses
- Disaster-specific queries
- Suggested questions
- Message history

### 7. Data Upload

- Drag-and-drop file upload
- Support for CSV, JSON, Excel, GeoJSON
- File validation
- Upload progress tracking
- Batch processing

## Customization

### Theme Colors

Edit `tailwind.config.js` to customize the color palette:

```javascript
theme: {
  extend: {
    colors: {
      primary: { /* your colors */ },
      danger: { /* your colors */ },
      // ...
    }
  }
}
```

### MBTI Profiles

Customize personality-based UI in `src/utils/mbtiStyles.js`:

```javascript
export const mbtiProfiles = {
  INTJ: {
    theme: { primary: 'indigo', accent: 'purple' },
    preferences: { /* settings */ },
    features: [ /* recommended features */ ],
  },
  // ...
}
```

## API Integration

The frontend communicates with the backend through:

1. **REST API** - CRUD operations, data fetching
2. **WebSocket** - Real-time updates, collaboration
3. **File Upload** - Multipart form data

API client configuration in `src/services/api.js`:

```javascript
const api = axios.create({
  baseURL: `${API_URL}/api/v1`,
  headers: { 'Content-Type': 'application/json' },
});
```

## Browser Support

- Chrome/Edge (latest)
- Firefox (latest)
- Safari (latest)
- Mobile browsers (iOS Safari, Chrome Mobile)

## Performance Optimization

- Code splitting with dynamic imports
- Lazy loading for routes and components
- Vendor chunk separation
- Image optimization
- Gzip compression (nginx)
- Service worker caching (optional)

## Security

- JWT token management
- XSS protection
- CSRF tokens
- Secure cookie handling
- Content Security Policy headers
- Input validation and sanitization

## Accessibility

- ARIA labels and roles
- Keyboard navigation
- Screen reader support
- Color contrast compliance
- Focus management

## Contributing

1. Follow the existing code structure
2. Use ESLint for code quality
3. Test responsive design
4. Ensure dark mode compatibility
5. Document new components

## License

Copyright (c) 2024 Disaster Resilience Hub. All rights reserved.

## Support

For issues or questions, please contact the development team or create an issue in the repository.
