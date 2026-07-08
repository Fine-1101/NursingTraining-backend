package org.example.nursingtrainingbackend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.example.nursingtrainingbackend.config.properties.JwtProperties;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
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
}
