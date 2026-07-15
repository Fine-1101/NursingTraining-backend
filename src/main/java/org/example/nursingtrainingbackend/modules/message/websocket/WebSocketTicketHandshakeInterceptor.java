package org.example.nursingtrainingbackend.modules.message.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * Validates and consumes the one-time ticket used by a learner to establish a
 * WebSocket connection.
 *
 * <p>The ticket issuer must store a JSON value under {@code ws:ticket:{ticket}}
 * with the following shape:</p>
 *
 * <pre>{@code
 * {"userId":100,"role":"1"}
 * }</pre>
 *
 * <p>Role {@code 1} is the learner role used by the current project. Redis
 * {@code GETDEL} is used through {@link StringRedisTemplate#getAndDelete(Object)}
 * so a ticket can be consumed only once.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketTicketHandshakeInterceptor implements HandshakeInterceptor {

    public static final String USER_ID_ATTRIBUTE = "userId";
    public static final String USER_ROLE_ATTRIBUTE = "userRole";

    private static final String TICKET_PARAMETER = "ticket";
    private static final String TICKET_KEY_PREFIX = "ws:ticket:";
    private static final String LEARNER_ROLE = "1";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {

        String ticket = extractSingleTicket(request);
        if (ticket == null || ticket.isBlank()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {
            String ticketValue = redisTemplate.opsForValue()
                    .getAndDelete(TICKET_KEY_PREFIX + ticket);

            if (ticketValue == null) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            TicketPrincipal principal = objectMapper.readValue(
                    ticketValue,
                    TicketPrincipal.class
            );

            if (principal.userId() == null
                    || principal.userId() <= 0
                    || !LEARNER_ROLE.equals(principal.role())) {
                log.warn("Rejected WebSocket ticket with invalid learner principal");
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return false;
            }

            attributes.put(USER_ID_ATTRIBUTE, principal.userId());
            attributes.put(USER_ROLE_ATTRIBUTE, principal.role());
            return true;
        } catch (Exception exception) {
            // Do not log the ticket or its Redis value because both are credentials.
            log.warn("Failed to validate WebSocket ticket", exception);
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
        // No post-handshake work is required.
    }

    private String extractSingleTicket(ServerHttpRequest request) {
        MultiValueMap<String, String> parameters = UriComponentsBuilder
                .fromUri(request.getURI())
                .build()
                .getQueryParams();

        if (parameters.get(TICKET_PARAMETER) == null
                || parameters.get(TICKET_PARAMETER).size() != 1) {
            return null;
        }
        return parameters.getFirst(TICKET_PARAMETER);
    }

    private record TicketPrincipal(Long userId, String role) {
    }
}
