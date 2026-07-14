package org.example.nursingtrainingbackend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.example.nursingtrainingbackend.config.properties.JwtProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class JwtService {
    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    private final JwtProperties properties;
    private final SecretKey key;
    private final StringRedisTemplate redisTemplate;

    public JwtService(JwtProperties properties, StringRedisTemplate redisTemplate) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(AuthenticatedUser user) {
        Instant now = Instant.now();
        return Jwts.builder().subject(user.id().toString()).issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.expiration())))
                .claim("username", user.username()).claim("nickname", user.nickname())
                .claim("role", user.role()).signWith(key).compact();
    }

    public AuthenticatedUser parseToken(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return new AuthenticatedUser(Long.valueOf(claims.getSubject()), claims.get("username", String.class),
                claims.get("nickname", String.class), claims.get("role", String.class));
    }

    public long expirationSeconds() {
        return properties.expiration().toSeconds();
    }

    /**
     * 将 Token 加入黑名单（登出时调用）
     */
    public void blacklist(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
            long remainingMs = claims.getExpiration().getTime() - System.currentTimeMillis();
            if (remainingMs > 0) {
                redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "1",
                        remainingMs, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            log.warn("Token 加黑失败，可能已过期", e);
        }
    }

    /**
     * 检查 Token 是否已被拉黑
     * Redis 不可用时返回 false（降级处理，不影响正常认证）
     */
    public boolean isBlacklisted(String token) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
        } catch (Exception e) {
            log.warn("Redis 不可用，跳过黑名单检查", e);
            return false;
        }
    }
}
