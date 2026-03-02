package com.disaster.collaboration.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Model representing a user action in a collaboration session
 * Used for real-time WebSocket messaging and presence tracking
 * Not persisted to database - transient event model
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAction {

    private String actionId;
    private String sessionId;
    private String userId;
    private String userName;
    private String userMbti;
    private ActionType type;
    private String payload;
    private Map<String, Object> data;
    private LocalDateTime timestamp;
    private String cursorPosition;
    private String currentView;

    public enum ActionType {
        // Session actions
        JOIN,
        LEAVE,
        RECONNECT,

        // Presence actions
        CURSOR_MOVE,
        VIEW_CHANGE,
        TYPING_START,
        TYPING_STOP,
        IDLE,
        ACTIVE,

        // Collaboration actions
        ANNOTATION_CREATE,
        ANNOTATION_UPDATE,
        ANNOTATION_DELETE,
        ANNOTATION_RESOLVE,
        ANNOTATION_VOTE,

        // Map actions
        MAP_PAN,
        MAP_ZOOM,
        MAP_LAYER_TOGGLE,

        // Communication actions
        MESSAGE,
        BROADCAST,
        MENTION,

        // Plan actions
        PLAN_UPDATE,
        ROUTE_MODIFY,
        RESOURCE_UPDATE,

        // System actions
        SYNC_REQUEST,
        SYNC_RESPONSE,
        HEARTBEAT,
        ERROR
    }

    public static UserAction heartbeat(String sessionId, String userId) {
        return UserAction.builder()
                .actionId(java.util.UUID.randomUUID().toString())
                .sessionId(sessionId)
                .userId(userId)
                .type(ActionType.HEARTBEAT)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static UserAction join(String sessionId, String userId, String userName, String mbti) {
        return UserAction.builder()
                .actionId(java.util.UUID.randomUUID().toString())
                .sessionId(sessionId)
                .userId(userId)
                .userName(userName)
                .userMbti(mbti)
                .type(ActionType.JOIN)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static UserAction leave(String sessionId, String userId, String userName) {
        return UserAction.builder()
                .actionId(java.util.UUID.randomUUID().toString())
                .sessionId(sessionId)
                .userId(userId)
                .userName(userName)
                .type(ActionType.LEAVE)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static UserAction cursorMove(String sessionId, String userId, String position) {
        return UserAction.builder()
                .actionId(java.util.UUID.randomUUID().toString())
                .sessionId(sessionId)
                .userId(userId)
                .type(ActionType.CURSOR_MOVE)
                .cursorPosition(position)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static UserAction message(String sessionId, String userId, String userName, String message) {
        return UserAction.builder()
                .actionId(java.util.UUID.randomUUID().toString())
                .sessionId(sessionId)
                .userId(userId)
                .userName(userName)
                .type(ActionType.MESSAGE)
                .payload(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
