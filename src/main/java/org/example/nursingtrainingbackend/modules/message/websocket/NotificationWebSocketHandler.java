package org.example.nursingtrainingbackend.modules.message.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maintains learner WebSocket sessions and sends server-side notifications.
 * The connection is intentionally read-only from the learner's perspective.
 */
@Slf4j
@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private static final int SEND_TIME_LIMIT_MILLIS = 10_000;
    private static final int BUFFER_SIZE_LIMIT_BYTES = 512 * 1024;

    private final ObjectMapper objectMapper;
    private final Map<Long, Set<WebSocketSession>> sessionsByUser =
            new ConcurrentHashMap<>();

    public NotificationWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = getUserId(session);
        if (userId == null) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Missing user identity"));
            return;
        }

        WebSocketSession concurrentSession = new ConcurrentWebSocketSessionDecorator(
                session,
                SEND_TIME_LIMIT_MILLIS,
                BUFFER_SIZE_LIMIT_BYTES
        );
        sessionsByUser
                .computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet())
                .add(concurrentSession);

        sendEvent(concurrentSession, Map.of(
                "event", "CONNECTED",
                "occurredAt", now()
        ));
        log.debug("Learner WebSocket connected: userId={}, sessionId={}",
                userId, session.getId());
    }

    /**
     * Learners are not allowed to send business messages on this connection.
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message)
            throws Exception {
        log.debug("Closing read-only WebSocket after client text message: sessionId={}",
                session.getId());
        session.close(CloseStatus.POLICY_VIOLATION.withReason("Read-only connection"));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        removeSession(session);
        log.debug("Learner WebSocket closed: sessionId={}, status={}",
                session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception)
            throws Exception {
        log.debug("Learner WebSocket transport error: sessionId={}",
                session.getId(), exception);
        removeSession(session);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    /**
     * Sends a payload to every live browser/device session of the learner.
     *
     * @return number of sessions to which the payload was sent successfully
     */
    public int sendToUser(Long userId, Object payload) {
        if (userId == null || payload == null) {
            return 0;
        }

        Set<WebSocketSession> sessions = sessionsByUser.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return 0;
        }

        int sentCount = 0;
        for (WebSocketSession session : Set.copyOf(sessions)) {
            if (sendEvent(session, payload)) {
                sentCount++;
            } else {
                removeSession(session);
                closeQuietly(session);
            }
        }
        return sentCount;
    }

    public boolean isUserOnline(Long userId) {
        Set<WebSocketSession> sessions = sessionsByUser.get(userId);
        return sessions != null && sessions.stream().anyMatch(WebSocketSession::isOpen);
    }

    public int onlineSessionCount() {
        return sessionsByUser.values().stream().mapToInt(Set::size).sum();
    }

    /**
     * Keeps idle connections alive through proxies. The client does not need to
     * reply with a business message; native WebSocket ping/pong is handled by
     * the WebSocket implementation.
     */
    @Scheduled(fixedDelayString = "${app.websocket.heartbeat-interval:30s}")
    public void sendHeartbeat() {
        Map<String, Object> heartbeat = Map.of(
                "event", "PING",
                "occurredAt", now()
        );

        for (Set<WebSocketSession> sessions : sessionsByUser.values()) {
            for (WebSocketSession session : Set.copyOf(sessions)) {
                if (!sendEvent(session, heartbeat)) {
                    removeSession(session);
                    closeQuietly(session);
                }
            }
        }
    }

    private boolean sendEvent(WebSocketSession session, Object payload) {
        if (!session.isOpen()) {
            return false;
        }
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
            return true;
        } catch (Exception exception) {
            log.debug("Failed to send WebSocket event: sessionId={}",
                    session.getId(), exception);
            return false;
        }
    }

    private void removeSession(WebSocketSession session) {
        Long userId = getUserId(session);
        if (userId == null) {
            return;
        }

        Set<WebSocketSession> sessions = sessionsByUser.get(userId);
        if (sessions == null) {
            return;
        }

        sessions.removeIf(candidate ->
                candidate.getId().equals(session.getId()) || !candidate.isOpen());
        if (sessions.isEmpty()) {
            sessionsByUser.remove(userId, sessions);
        }
    }

    private Long getUserId(WebSocketSession session) {
        Object value = session.getAttributes()
                .get(WebSocketTicketHandshakeInterceptor.USER_ID_ATTRIBUTE);
        return value instanceof Long userId ? userId : null;
    }

    private void closeQuietly(WebSocketSession session) {
        if (!session.isOpen()) {
            return;
        }
        try {
            session.close(CloseStatus.SERVER_ERROR);
        } catch (IOException exception) {
            log.debug("Failed to close WebSocket session: sessionId={}",
                    session.getId(), exception);
        }
    }

    private String now() {
        return OffsetDateTime.now(ZoneOffset.ofHours(8)).toString();
    }
}
