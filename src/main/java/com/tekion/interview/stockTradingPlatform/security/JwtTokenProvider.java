package com.tekion.interview.stockTradingPlatform.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long sessionTokenTtlSeconds;

    public JwtTokenProvider(
            @Value("${app.security.jwt-secret}") String secret,
            @Value("${app.security.session-token-ttl-seconds}") long sessionTokenTtlSeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.sessionTokenTtlSeconds = sessionTokenTtlSeconds;
    }

    public String issueSessionToken(UUID userId, String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + sessionTokenTtlSeconds * 1000);
        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public long getSessionTokenTtlSeconds() {
        return sessionTokenTtlSeconds;
    }

    /** @return the authenticated userId. Throws JwtException (401 via GlobalExceptionHandler) if invalid/expired. */
    public UUID validateAndGetUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return UUID.fromString(claims.getSubject());
    }
}
