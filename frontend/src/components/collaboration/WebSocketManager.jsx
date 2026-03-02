import { useEffect } from 'react';
import { useWebSocket } from '@hooks/useWebSocket';
import { useAuth } from '@hooks/useAuth';
import { toast } from 'react-toastify';

const WebSocketManager = ({ children }) => {
  const { isAuthenticated } = useAuth();
  const { on, off, isConnected } = useWebSocket();

  useEffect(() => {
    if (isAuthenticated && isConnected) {
      setupGlobalListeners();

      return () => {
        cleanupGlobalListeners();
      };
    }
  }, [isAuthenticated, isConnected]);

  const setupGlobalListeners = () => {
    on('disaster_alert', handleDisasterAlert);
    on('evacuation_update', handleEvacuationUpdate);
    on('system_notification', handleSystemNotification);
  };

  const cleanupGlobalListeners = () => {
    off('disaster_alert', handleDisasterAlert);
    off('evacuation_update', handleEvacuationUpdate);
    off('system_notification', handleSystemNotification);
  };

  const handleDisasterAlert = (data) => {
    toast.warning(
      <div>
        <div className="font-bold">Disaster Alert</div>
        <div>{data.message}</div>
      </div>,
      {
        autoClose: false,
        closeButton: true,
      }
    );
  };

  const handleEvacuationUpdate = (data) => {
    toast.info(
      <div>
        <div className="font-bold">Evacuation Update</div>
        <div>{data.message}</div>
      </div>
    );
  };

  const handleSystemNotification = (data) => {
    toast.info(data.message);
  };

  return <>{children}</>;
};

export default WebSocketManager;
