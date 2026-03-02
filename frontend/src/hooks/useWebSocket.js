import { useEffect, useRef, useCallback } from 'react';
import wsService from '@services/websocket';
import { useAuth } from './useAuth';

export const useWebSocket = () => {
  const { isAuthenticated } = useAuth();
  const listenersRef = useRef([]);

  useEffect(() => {
    if (isAuthenticated) {
      const token = localStorage.getItem('token');
      wsService.connect(token);

      return () => {
        // Clean up listeners on unmount
        listenersRef.current.forEach(({ event, callback }) => {
          wsService.off(event, callback);
        });
        listenersRef.current = [];
      };
    }
  }, [isAuthenticated]);

  const on = useCallback((event, callback) => {
    wsService.on(event, callback);
    listenersRef.current.push({ event, callback });
  }, []);

  const off = useCallback((event, callback) => {
    wsService.off(event, callback);
    listenersRef.current = listenersRef.current.filter(
      (listener) => !(listener.event === event && listener.callback === callback)
    );
  }, []);

  const emit = useCallback((event, data) => {
    wsService.emit(event, data);
  }, []);

  const joinRoom = useCallback((roomId) => {
    wsService.joinRoom(roomId);
  }, []);

  const leaveRoom = useCallback((roomId) => {
    wsService.leaveRoom(roomId);
  }, []);

  const sendMessage = useCallback((roomId, message) => {
    wsService.sendMessage(roomId, message);
  }, []);

  return {
    on,
    off,
    emit,
    joinRoom,
    leaveRoom,
    sendMessage,
    isConnected: wsService.socket?.connected || false,
  };
};
