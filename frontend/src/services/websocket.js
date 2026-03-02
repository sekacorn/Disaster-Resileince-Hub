import { io } from 'socket.io-client';

const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8000';

class WebSocketService {
  constructor() {
    this.socket = null;
    this.listeners = new Map();
  }

  connect(token) {
    if (this.socket?.connected) {
      return;
    }

    this.socket = io(WS_URL, {
      auth: {
        token: token || localStorage.getItem('token'),
      },
      transports: ['websocket', 'polling'],
      reconnection: true,
      reconnectionDelay: 1000,
      reconnectionDelayMax: 5000,
      reconnectionAttempts: 5,
    });

    this.socket.on('connect', () => {
      console.log('WebSocket connected');
    });

    this.socket.on('disconnect', (reason) => {
      console.log('WebSocket disconnected:', reason);
    });

    this.socket.on('error', (error) => {
      console.error('WebSocket error:', error);
    });

    this.socket.on('connect_error', (error) => {
      console.error('WebSocket connection error:', error);
    });
  }

  disconnect() {
    if (this.socket) {
      this.socket.disconnect();
      this.socket = null;
      this.listeners.clear();
    }
  }

  on(event, callback) {
    if (!this.socket) {
      console.error('WebSocket not connected');
      return;
    }

    if (!this.listeners.has(event)) {
      this.listeners.set(event, []);
    }

    this.listeners.get(event).push(callback);
    this.socket.on(event, callback);
  }

  off(event, callback) {
    if (!this.socket) {
      return;
    }

    this.socket.off(event, callback);

    if (this.listeners.has(event)) {
      const callbacks = this.listeners.get(event).filter((cb) => cb !== callback);
      if (callbacks.length === 0) {
        this.listeners.delete(event);
      } else {
        this.listeners.set(event, callbacks);
      }
    }
  }

  emit(event, data) {
    if (!this.socket?.connected) {
      console.error('WebSocket not connected');
      return;
    }

    this.socket.emit(event, data);
  }

  // Collaboration-specific methods
  joinRoom(roomId) {
    this.emit('join_room', { room_id: roomId });
  }

  leaveRoom(roomId) {
    this.emit('leave_room', { room_id: roomId });
  }

  sendMessage(roomId, message) {
    this.emit('chat_message', { room_id: roomId, message });
  }

  updateCursor(roomId, position) {
    this.emit('cursor_update', { room_id: roomId, position });
  }

  // Disaster alerts
  subscribeToAlerts(filters = {}) {
    this.emit('subscribe_alerts', filters);
  }

  unsubscribeFromAlerts() {
    this.emit('unsubscribe_alerts');
  }

  // Real-time data updates
  subscribeToDataUpdates(datasetId) {
    this.emit('subscribe_data', { dataset_id: datasetId });
  }

  unsubscribeFromDataUpdates(datasetId) {
    this.emit('unsubscribe_data', { dataset_id: datasetId });
  }
}

const wsService = new WebSocketService();

export default wsService;
