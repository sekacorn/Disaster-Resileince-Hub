import { useState, useEffect, useRef } from 'react';
import { useWebSocket } from '@hooks/useWebSocket';
import { useAuth } from '@hooks/useAuth';
import { collaborationAPI } from '@services/api';
import { toast } from 'react-toastify';
import { FaPaperPlane, FaUsers, FaCircle } from 'react-icons/fa';

const CollabPanel = ({ room, onLeave }) => {
  const { user } = useAuth();
  const { on, off, sendMessage: wsSendMessage } = useWebSocket();
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [activeUsers, setActiveUsers] = useState([]);
  const messagesEndRef = useRef(null);

  useEffect(() => {
    loadMessages();
    setupWebSocketListeners();

    return () => {
      cleanupWebSocketListeners();
    };
  }, [room]);

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const loadMessages = async () => {
    try {
      const response = await collaborationAPI.getRoom(room.id);
      setMessages(response.data.messages || []);
      setActiveUsers(response.data.active_users || []);
    } catch (error) {
      toast.error('Failed to load messages');
    }
  };

  const setupWebSocketListeners = () => {
    on('chat_message', handleNewMessage);
    on('user_joined', handleUserJoined);
    on('user_left', handleUserLeft);
    on('typing', handleTyping);
  };

  const cleanupWebSocketListeners = () => {
    off('chat_message', handleNewMessage);
    off('user_joined', handleUserJoined);
    off('user_left', handleUserLeft);
    off('typing', handleTyping);
  };

  const handleNewMessage = (data) => {
    if (data.room_id === room.id) {
      setMessages((prev) => [...prev, data.message]);
    }
  };

  const handleUserJoined = (data) => {
    if (data.room_id === room.id) {
      setActiveUsers((prev) => [...prev, data.user]);
      toast.info(`${data.user.full_name} joined the room`);
    }
  };

  const handleUserLeft = (data) => {
    if (data.room_id === room.id) {
      setActiveUsers((prev) => prev.filter((u) => u.id !== data.user_id));
      toast.info(`${data.user_name} left the room`);
    }
  };

  const handleTyping = (data) => {
    // Handle typing indicator
    console.log('User typing:', data);
  };

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const handleSendMessage = async (e) => {
    e.preventDefault();
    if (!input.trim()) return;

    const messageData = {
      content: input,
      user_id: user.id,
      user_name: user.full_name || user.email,
      timestamp: new Date().toISOString(),
    };

    try {
      await collaborationAPI.sendMessage(room.id, messageData);
      wsSendMessage(room.id, messageData);
      setInput('');
    } catch (error) {
      toast.error('Failed to send message');
    }
  };

  return (
    <div className="h-full flex flex-col">
      {/* Header */}
      <div className="bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 p-4">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-xl font-bold">{room.name}</h2>
            <p className="text-sm text-gray-600 dark:text-gray-400">
              {activeUsers.length} active users
            </p>
          </div>
          <button onClick={onLeave} className="btn btn-secondary">
            Leave Room
          </button>
        </div>
      </div>

      <div className="flex-1 flex overflow-hidden">
        {/* Messages Area */}
        <div className="flex-1 flex flex-col">
          {/* Messages List */}
          <div className="flex-1 overflow-y-auto p-4 space-y-4">
            {messages.map((message, index) => {
              const isOwnMessage = message.user_id === user?.id;
              return (
                <div
                  key={index}
                  className={`flex gap-3 ${
                    isOwnMessage ? 'justify-end' : 'justify-start'
                  }`}
                >
                  {!isOwnMessage && (
                    <div className="w-8 h-8 rounded-full bg-primary-100 dark:bg-primary-900 flex items-center justify-center flex-shrink-0">
                      <span className="text-sm font-bold text-primary-600 dark:text-primary-400">
                        {message.user_name?.charAt(0).toUpperCase()}
                      </span>
                    </div>
                  )}
                  <div
                    className={`max-w-md rounded-lg p-3 ${
                      isOwnMessage
                        ? 'bg-primary-600 text-white'
                        : 'bg-white dark:bg-gray-800'
                    }`}
                  >
                    {!isOwnMessage && (
                      <div className="text-xs font-medium mb-1 text-gray-600 dark:text-gray-400">
                        {message.user_name}
                      </div>
                    )}
                    <div className="whitespace-pre-wrap">{message.content}</div>
                    <div
                      className={`text-xs mt-1 ${
                        isOwnMessage
                          ? 'text-primary-100'
                          : 'text-gray-500 dark:text-gray-400'
                      }`}
                    >
                      {new Date(message.timestamp).toLocaleTimeString()}
                    </div>
                  </div>
                  {isOwnMessage && (
                    <div className="w-8 h-8 rounded-full bg-gray-200 dark:bg-gray-700 flex items-center justify-center flex-shrink-0">
                      <span className="text-sm font-bold text-gray-600 dark:text-gray-400">
                        {user.full_name?.charAt(0).toUpperCase() || 'U'}
                      </span>
                    </div>
                  )}
                </div>
              );
            })}
            <div ref={messagesEndRef} />
          </div>

          {/* Input Area */}
          <div className="bg-white dark:bg-gray-800 border-t border-gray-200 dark:border-gray-700 p-4">
            <form onSubmit={handleSendMessage} className="flex gap-2">
              <input
                type="text"
                value={input}
                onChange={(e) => setInput(e.target.value)}
                placeholder="Type a message..."
                className="input flex-1"
              />
              <button
                type="submit"
                disabled={!input.trim()}
                className="btn btn-primary"
              >
                <FaPaperPlane />
              </button>
            </form>
          </div>
        </div>

        {/* Active Users Sidebar */}
        <div className="w-64 bg-white dark:bg-gray-800 border-l border-gray-200 dark:border-gray-700 p-4">
          <h3 className="font-bold mb-4 flex items-center">
            <FaUsers className="mr-2" />
            Active Users
          </h3>
          <div className="space-y-2">
            {activeUsers.map((activeUser) => (
              <div
                key={activeUser.id}
                className="flex items-center gap-3 p-2 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-900"
              >
                <div className="relative">
                  <div className="w-10 h-10 rounded-full bg-primary-100 dark:bg-primary-900 flex items-center justify-center">
                    <span className="text-sm font-bold text-primary-600 dark:text-primary-400">
                      {activeUser.full_name?.charAt(0).toUpperCase()}
                    </span>
                  </div>
                  <FaCircle className="absolute bottom-0 right-0 w-3 h-3 text-success-500" />
                </div>
                <div className="flex-1">
                  <div className="font-medium text-sm">
                    {activeUser.full_name || activeUser.email}
                    {activeUser.id === user?.id && ' (You)'}
                  </div>
                  <div className="text-xs text-gray-500">
                    {activeUser.role?.replace('_', ' ')}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default CollabPanel;
