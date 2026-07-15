package org.example.nursingtrainingbackend.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "app.websocket")
public record WebSocketProperties(
        String endpoint,
        Duration ticketExpiration,
        Duration idleTimeout,
        Duration heartbeatInterval,
        List<String> allowedOrigins
) {
}
