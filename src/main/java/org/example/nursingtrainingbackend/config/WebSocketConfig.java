package org.example.nursingtrainingbackend.config;

import lombok.RequiredArgsConstructor;
import org.example.nursingtrainingbackend.config.properties.WebSocketProperties;
import org.example.nursingtrainingbackend.modules.message.websocket.NotificationWebSocketHandler;
import org.example.nursingtrainingbackend.modules.message.websocket.WebSocketTicketHandshakeInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration(proxyBeanMethods = false)
@EnableWebSocket
@EnableConfigurationProperties(WebSocketProperties.class)
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final NotificationWebSocketHandler handler;
    private final WebSocketTicketHandshakeInterceptor interceptor;
    private final WebSocketProperties properties;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, properties.endpoint())
                .addInterceptors(interceptor)
                .setAllowedOrigins(properties.allowedOrigins().toArray(String[]::new));
    }
}
