package org.example.nursingtrainingbackend.security;

import io.jsonwebtoken.JwtException;
import org.example.nursingtrainingbackend.config.properties.JwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

class JwtServiceTests {
    private static final String SECRET = "a-secure-test-key-with-at-least-thirty-two-bytes";
    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

    private JwtService newService(Duration expiration) {
        return new JwtService(new JwtProperties(SECRET, expiration), redisTemplate);
    }

    @Test
    void createsAndParsesToken() {
        JwtService service = newService(Duration.ofHours(2));
        AuthenticatedUser expected = new AuthenticatedUser(7L, "nurse", "护士", "STUDENT");
        String token = service.createToken(expected);
        assertThat(service.parseToken(token)).isEqualTo(expected);
        assertThat(service.expirationSeconds()).isEqualTo(7200);
    }

    @Test
    void rejectsTamperedToken() {
        JwtService service = newService(Duration.ofHours(2));
        String token = service.createToken(new AuthenticatedUser(1L, "admin", "管理员", "ADMIN"));
        String[] parts = token.split("\\.");
        String payload = parts[1];
        char replacement = payload.charAt(0) == 'a' ? 'b' : 'a';
        String tampered = parts[0] + "." + replacement + payload.substring(1) + "." + parts[2];
        assertThatThrownBy(() -> service.parseToken(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsExpiredToken() {
        JwtService service = newService(Duration.ofSeconds(-1));
        String token = service.createToken(new AuthenticatedUser(1L, "admin", "管理员", "ADMIN"));
        assertThatThrownBy(() -> service.parseToken(token)).isInstanceOf(JwtException.class);
    }
}
