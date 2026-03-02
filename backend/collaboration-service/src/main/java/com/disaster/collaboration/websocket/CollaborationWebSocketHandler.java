package com.disaster.collaboration.websocket;

import com.disaster.collaboration.model.UserAction;
import com.disaster.collaboration.service.PresenceService;
import com.disaster.collaboration.service.SessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket handler for real-time collaboration features
 * Manages bidirectional communication between clients and server
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CollaborationWebSocketHandler extends TextWebSocketHandler {

    private final PresenceService presenceService;
    private final SessionService sessionService;
    private final ObjectMapper objectMapper;

    // Map of sessionId -> Set of WebSocket sessions
    private final Map<String, CopyOnWriteArraySet<WebSocketSession>> sessionConnections = new ConcurrentHashMap<>();

    // Map of WebSocket sessionId -> collaboration sessionId
    private final Map<String, String> wsSessionToCollabSession = new ConcurrentHashMap<>();

    // Map of WebSocket sessionId -> userId
    private final Map<String, String> wsSessionToUserId = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket connection established: {}", session.getId());

        // Extract sessionId from URI
        String sessionId = extractSessionId(session);
        if (sessionId == null) {
            session.close(CloseStatus.BAD_DATA.withReason("Invalid session ID"));
            return;
        }

        // Add to session connections
        sessionConnections.computeIfAbsent(sessionId, k -> new CopyOnWriteArraySet<>()).add(session);
        wsSessionToCollabSession.put(session.getId(), sessionId);

        log.info("WebSocket session {} joined collaboration session {}", session.getId(), sessionId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("Received WebSocket message: {}", payload);

        try {
            UserAction action = objectMapper.readValue(payload, UserAction.class);
            String sessionId = wsSessionToCollabSession.get(session.getId());

            if (sessionId == null) {
                log.warn("No collaboration session found for WebSocket session: {}", session.getId());
                return;
            }

            // Handle different action types
            handleUserAction(sessionId, session, action);

        } catch (Exception e) {
            log.error("Error handling WebSocket message", e);
            sendError(session, "Failed to process message: " + e.getMessage());
        }
    }

    private void handleUserAction(String sessionId, WebSocketSession wsSession, UserAction action) throws IOException {
        switch (action.getType()) {
            case JOIN -> handleJoin(sessionId, wsSession, action);
            case LEAVE -> handleLeave(sessionId, wsSession, action);
            case CURSOR_MOVE -> handleCursorMove(sessionId, wsSession, action);
            case VIEW_CHANGE -> handleViewChange(sessionId, wsSession, action);
            case TYPING_START, TYPING_STOP -> handleTyping(sessionId, wsSession, action);
            case MESSAGE -> handleMessage(sessionId, wsSession, action);
            case BROADCAST -> handleBroadcast(sessionId, wsSession, action);
            case HEARTBEAT -> handleHeartbeat(sessionId, wsSession, action);
            case ANNOTATION_CREATE, ANNOTATION_UPDATE, ANNOTATION_DELETE, ANNOTATION_RESOLVE, ANNOTATION_VOTE ->
                    handleAnnotationAction(sessionId, wsSession, action);
            case MAP_PAN, MAP_ZOOM, MAP_LAYER_TOGGLE ->
                    handleMapAction(sessionId, wsSession, action);
            case PLAN_UPDATE, ROUTE_MODIFY, RESOURCE_UPDATE ->
                    handlePlanAction(sessionId, wsSession, action);
            case SYNC_REQUEST -> handleSyncRequest(sessionId, wsSession, action);
            default -> log.warn("Unknown action type: {}", action.getType());
        }
    }

    private void handleJoin(String sessionId, WebSocketSession wsSession, UserAction action) throws IOException {
        String userId = action.getUserId();
        String userName = action.getUserName();
        String mbti = action.getUserMbti();

        // Register participant in database
        presenceService.joinSession(sessionId, userId, userName, mbti);
        presenceService.setWebSocketSession(sessionId, userId, wsSession.getId());

        // Store mapping
        wsSessionToUserId.put(wsSession.getId(), userId);

        // Notify all participants
        UserAction joinNotification = UserAction.builder()
                .actionId(java.util.UUID.randomUUID().toString())
                .sessionId(sessionId)
                .userId(userId)
                .userName(userName)
                .userMbti(mbti)
                .type(UserAction.ActionType.JOIN)
                .timestamp(LocalDateTime.now())
                .build();

        broadcastToSession(sessionId, joinNotification, wsSession.getId());

        // Send current participants to new user
        sendCurrentState(sessionId, wsSession);

        log.info("User {} joined session {}", userId, sessionId);
    }

    private void handleLeave(String sessionId, WebSocketSession wsSession, UserAction action) throws IOException {
        String userId = wsSessionToUserId.get(wsSession.getId());
        if (userId != null) {
            presenceService.leaveSession(sessionId, userId);

            UserAction leaveNotification = UserAction.leave(sessionId, userId, action.getUserName());
            broadcastToSession(sessionId, leaveNotification, wsSession.getId());

            log.info("User {} left session {}", userId, sessionId);
        }
    }

    private void handleCursorMove(String sessionId, WebSocketSession wsSession, UserAction action) throws IOException {
        String userId = wsSessionToUserId.get(wsSession.getId());
        if (userId != null) {
            presenceService.updatePresence(sessionId, userId, action.getCursorPosition(), null);
            broadcastToSession(sessionId, action, wsSession.getId());
        }
    }

    private void handleViewChange(String sessionId, WebSocketSession wsSession, UserAction action) throws IOException {
        String userId = wsSessionToUserId.get(wsSession.getId());
        if (userId != null) {
            presenceService.updatePresence(sessionId, userId, null, action.getCurrentView());
            broadcastToSession(sessionId, action, wsSession.getId());
        }
    }

    private void handleTyping(String sessionId, WebSocketSession wsSession, UserAction action) throws IOException {
        broadcastToSession(sessionId, action, wsSession.getId());
    }

    private void handleMessage(String sessionId, WebSocketSession wsSession, UserAction action) throws IOException {
        broadcastToSession(sessionId, action, null); // Include sender
    }

    private void handleBroadcast(String sessionId, WebSocketSession wsSession, UserAction action) throws IOException {
        broadcastToSession(sessionId, action, null); // Include sender
    }

    private void handleHeartbeat(String sessionId, WebSocketSession wsSession, UserAction action) {
        String userId = wsSessionToUserId.get(wsSession.getId());
        if (userId != null) {
            presenceService.heartbeat(sessionId, userId);
        }
    }

    private void handleAnnotationAction(String sessionId, WebSocketSession wsSession, UserAction action) throws IOException {
        // Update session activity
        sessionService.updateSessionActivity(sessionId);
        // Broadcast to all participants
        broadcastToSession(sessionId, action, null);
    }

    private void handleMapAction(String sessionId, WebSocketSession wsSession, UserAction action) throws IOException {
        broadcastToSession(sessionId, action, wsSession.getId());
    }

    private void handlePlanAction(String sessionId, WebSocketSession wsSession, UserAction action) throws IOException {
        sessionService.updateSessionActivity(sessionId);
        broadcastToSession(sessionId, action, null);
    }

    private void handleSyncRequest(String sessionId, WebSocketSession wsSession, UserAction action) throws IOException {
        sendCurrentState(sessionId, wsSession);
    }

    private void sendCurrentState(String sessionId, WebSocketSession wsSession) throws IOException {
        var participants = presenceService.getSessionParticipants(sessionId);
        var onlineUsers = presenceService.getOnlineUsers(sessionId);

        UserAction syncResponse = UserAction.builder()
                .actionId(java.util.UUID.randomUUID().toString())
                .sessionId(sessionId)
                .type(UserAction.ActionType.SYNC_RESPONSE)
                .data(Map.of(
                        "participants", participants,
                        "onlineUsers", onlineUsers
                ))
                .timestamp(LocalDateTime.now())
                .build();

        sendToSession(wsSession, syncResponse);
    }

    private void broadcastToSession(String sessionId, UserAction action, String excludeWsSessionId) throws IOException {
        CopyOnWriteArraySet<WebSocketSession> sessions = sessionConnections.get(sessionId);
        if (sessions != null) {
            String message = objectMapper.writeValueAsString(action);
            TextMessage textMessage = new TextMessage(message);

            for (WebSocketSession session : sessions) {
                if (excludeWsSessionId != null && session.getId().equals(excludeWsSessionId)) {
                    continue; // Skip sender
                }
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        log.error("Failed to send message to session {}", session.getId(), e);
                    }
                }
            }
        }
    }

    private void sendToSession(WebSocketSession session, UserAction action) throws IOException {
        if (session.isOpen()) {
            String message = objectMapper.writeValueAsString(action);
            session.sendMessage(new TextMessage(message));
        }
    }

    private void sendError(WebSocketSession session, String errorMessage) throws IOException {
        UserAction errorAction = UserAction.builder()
                .actionId(java.util.UUID.randomUUID().toString())
                .type(UserAction.ActionType.ERROR)
                .payload(errorMessage)
                .timestamp(LocalDateTime.now())
                .build();
        sendToSession(session, errorAction);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket connection closed: {} with status {}", session.getId(), status);

        String sessionId = wsSessionToCollabSession.remove(session.getId());
        String userId = wsSessionToUserId.remove(session.getId());

        if (sessionId != null) {
            // Remove from session connections
            CopyOnWriteArraySet<WebSocketSession> sessions = sessionConnections.get(sessionId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    sessionConnections.remove(sessionId);
                }
            }

            // Mark user as left
            if (userId != null) {
                try {
                    presenceService.leaveSession(sessionId, userId);

                    UserAction leaveNotification = UserAction.leave(sessionId, userId, "");
                    broadcastToSession(sessionId, leaveNotification, session.getId());
                } catch (Exception e) {
                    log.error("Error handling connection close", e);
                }
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error for session {}", session.getId(), exception);
        session.close(CloseStatus.SERVER_ERROR);
    }

    private String extractSessionId(WebSocketSession session) {
        String uri = session.getUri().toString();
        String[] parts = uri.split("/");
        return parts.length > 0 ? parts[parts.length - 1] : null;
    }
}
