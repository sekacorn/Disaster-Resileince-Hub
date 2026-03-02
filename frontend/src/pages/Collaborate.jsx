import { useState, useEffect } from 'react';
import CollabPanel from '@components/collaboration/CollabPanel';
import { collaborationAPI } from '@services/api';
import { useWebSocket } from '@hooks/useWebSocket';
import { toast } from 'react-toastify';
import { FaUsers, FaPlus } from 'react-icons/fa';

const Collaborate = () => {
  const [rooms, setRooms] = useState([]);
  const [selectedRoom, setSelectedRoom] = useState(null);
  const [loading, setLoading] = useState(true);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newRoomName, setNewRoomName] = useState('');
  const { joinRoom, leaveRoom } = useWebSocket();

  useEffect(() => {
    loadRooms();
  }, []);

  useEffect(() => {
    return () => {
      if (selectedRoom) {
        leaveRoom(selectedRoom.id);
      }
    };
  }, [selectedRoom]);

  const loadRooms = async () => {
    try {
      setLoading(true);
      const response = await collaborationAPI.getRooms();
      setRooms(response.data.rooms || []);
    } catch (error) {
      toast.error('Failed to load collaboration rooms');
    } finally {
      setLoading(false);
    }
  };

  const handleCreateRoom = async () => {
    if (!newRoomName.trim()) {
      toast.error('Please enter a room name');
      return;
    }

    try {
      const response = await collaborationAPI.createRoom({
        name: newRoomName,
      });
      setRooms([...rooms, response.data]);
      setNewRoomName('');
      setShowCreateModal(false);
      toast.success('Room created successfully');
    } catch (error) {
      toast.error('Failed to create room');
    }
  };

  const handleJoinRoom = async (room) => {
    try {
      if (selectedRoom) {
        leaveRoom(selectedRoom.id);
      }

      await collaborationAPI.joinRoom(room.id);
      setSelectedRoom(room);
      joinRoom(room.id);
      toast.success(`Joined ${room.name}`);
    } catch (error) {
      toast.error('Failed to join room');
    }
  };

  const handleLeaveRoom = async () => {
    if (!selectedRoom) return;

    try {
      await collaborationAPI.leaveRoom(selectedRoom.id);
      leaveRoom(selectedRoom.id);
      setSelectedRoom(null);
      toast.info('Left the room');
    } catch (error) {
      toast.error('Failed to leave room');
    }
  };

  return (
    <div className="h-full flex">
      {/* Left Sidebar - Room List */}
      <div className="w-80 bg-white dark:bg-gray-800 border-r border-gray-200 dark:border-gray-700 p-4 overflow-y-auto">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-bold flex items-center">
            <FaUsers className="mr-2" />
            Collaboration Rooms
          </h2>
          <button
            onClick={() => setShowCreateModal(true)}
            className="btn btn-primary btn-sm"
          >
            <FaPlus />
          </button>
        </div>

        {loading ? (
          <div className="flex justify-center py-8">
            <div className="spinner"></div>
          </div>
        ) : rooms.length > 0 ? (
          <div className="space-y-2">
            {rooms.map((room) => (
              <div
                key={room.id}
                onClick={() => handleJoinRoom(room)}
                className={`p-3 rounded-lg cursor-pointer transition-colors ${
                  selectedRoom?.id === room.id
                    ? 'bg-primary-100 dark:bg-primary-900 border-2 border-primary-500'
                    : 'bg-gray-50 dark:bg-gray-900 hover:bg-gray-100 dark:hover:bg-gray-800 border-2 border-transparent'
                }`}
              >
                <div className="font-medium">{room.name}</div>
                <div className="text-xs text-gray-500 mt-1">
                  {room.active_users || 0} active users
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="text-center py-8 text-gray-500">
            <FaUsers className="text-4xl mx-auto mb-2 opacity-20" />
            <p>No collaboration rooms</p>
            <button
              onClick={() => setShowCreateModal(true)}
              className="btn btn-primary mt-4"
            >
              Create First Room
            </button>
          </div>
        )}
      </div>

      {/* Main Content - Collaboration Panel */}
      <div className="flex-1">
        {selectedRoom ? (
          <CollabPanel room={selectedRoom} onLeave={handleLeaveRoom} />
        ) : (
          <div className="flex items-center justify-center h-full text-gray-500">
            <div className="text-center">
              <FaUsers className="text-6xl mx-auto mb-4 opacity-20" />
              <p className="text-xl">Select a room to start collaborating</p>
            </div>
          </div>
        )}
      </div>

      {/* Create Room Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white dark:bg-gray-800 rounded-lg p-6 w-96 shadow-xl">
            <h3 className="text-xl font-bold mb-4">Create Collaboration Room</h3>
            <input
              type="text"
              placeholder="Room name"
              value={newRoomName}
              onChange={(e) => setNewRoomName(e.target.value)}
              className="input mb-4"
              autoFocus
            />
            <div className="flex gap-2">
              <button onClick={handleCreateRoom} className="btn btn-primary flex-1">
                Create
              </button>
              <button
                onClick={() => {
                  setShowCreateModal(false);
                  setNewRoomName('');
                }}
                className="btn btn-secondary flex-1"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Collaborate;
