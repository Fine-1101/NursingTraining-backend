package org.example.nursingtrainingbackend.modules.message.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.example.nursingtrainingbackend.config.properties.WebSocketProperties;
import org.example.nursingtrainingbackend.modules.message.vo.TicketVO;
import org.example.nursingtrainingbackend.security.SecurityUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketTicketService {

    private static final String TICKET_KEY_PREFIX = "ws:ticket:";
    private static final String RATE_LIMIT_KEY_PREFIX = "ws:ticket:rate:";
    private static final int RATE_LIMIT_MAX_PER_MINUTE = 5;
    private static final int TICKET_BYTES = 32;

    private final StringRedisTemplate redisTemplate;
    private final WebSocketProperties webSocketProperties;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public TicketVO createTicket() {
        Long userId = SecurityUtils.currentUserId();
        String role = SecurityUtils.currentUser().role();

        if (role == null || Integer.parseInt(role) != 1) {
            throw new BusinessException(ErrorCode.NOT_STUDENT_ROLE);
        }

        String rateLimitKey = RATE_LIMIT_KEY_PREFIX + userId;
        Long count = redisTemplate.opsForValue().increment(rateLimitKey);
        if (count == 1) {
            redisTemplate.expire(rateLimitKey, 1, TimeUnit.MINUTES);
        }
        if (count > RATE_LIMIT_MAX_PER_MINUTE) {
            throw new BusinessException(ErrorCode.WS_TICKET_RATE_LIMITED);
        }

        byte[] randomBytes = new byte[TICKET_BYTES];
        secureRandom.nextBytes(randomBytes);
        String ticket = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        long expiresIn = webSocketProperties.ticketExpiration().getSeconds();

        try {
            Map<String, Object> ticketData = new HashMap<>();
            ticketData.put("userId", userId);
            ticketData.put("role", role);
            String json = objectMapper.writeValueAsString(ticketData);
            redisTemplate.opsForValue().set(TICKET_KEY_PREFIX + ticket, json,
                    expiresIn, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Failed to store WebSocket ticket in Redis", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }

        TicketVO vo = new TicketVO();
        vo.setTicket(ticket);
        vo.setExpiresIn(expiresIn);
        vo.setWebSocketPath(webSocketProperties.endpoint());
        return vo;
    }
}
