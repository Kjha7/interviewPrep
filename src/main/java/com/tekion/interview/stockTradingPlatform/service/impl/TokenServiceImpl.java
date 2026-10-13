package com.tekion.interview.stockTradingPlatform.service.impl;

import com.tekion.interview.stockTradingPlatform.dto.response.LoginResponse;
import com.tekion.interview.stockTradingPlatform.exception.InvalidCredentialsException;
import com.tekion.interview.stockTradingPlatform.security.JwtTokenProvider;
import com.tekion.interview.stockTradingPlatform.service.TokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Refresh tokens are opaque random strings, stored HASHED in Redis with native TTL -
 * never in Postgres (see design-doc feedback #1: a relational row for a 15-min-scale
 * credential means a DB hit on every validation and manual cleanup of expired rows).
 *
 * Key layout: refresh:{sha256(token)} -> "{userId}::{username}"
 * Rotation: refresh() deletes the old key and issues a brand-new refresh token, so a
 * stolen-and-replayed old refresh token fails after the legitimate client rotates it.
 */
@Service
public class TokenServiceImpl implements TokenService {

    private static final String REFRESH_KEY_PREFIX = "refresh:";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;
    private final long refreshTokenTtlSeconds;

    public TokenServiceImpl(
            JwtTokenProvider jwtTokenProvider,
            StringRedisTemplate redisTemplate,
            @Value("${app.security.refresh-token-ttl-seconds}") long refreshTokenTtlSeconds) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTemplate = redisTemplate;
        this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
    }

    @Override
    public LoginResponse issueTokenPair(UUID userId, String username) {
        String sessionToken = jwtTokenProvider.issueSessionToken(userId, username);
        String refreshToken = generateOpaqueToken();

        redisTemplate.opsForValue().set(
                REFRESH_KEY_PREFIX + sha256(refreshToken),
                userId + "::" + username,
                Duration.ofSeconds(refreshTokenTtlSeconds));

        return LoginResponse.builder()
                .userId(userId)
                .sessionToken(sessionToken)
                .sessionTokenExpiresIn(jwtTokenProvider.getSessionTokenTtlSeconds())
                .refreshToken(refreshToken)
                .refreshTokenExpiresIn(refreshTokenTtlSeconds)
                .build();
    }

    @Override
    public LoginResponse refresh(String refreshToken) {
        String redisKey = REFRESH_KEY_PREFIX + sha256(refreshToken);
        String stored = redisTemplate.opsForValue().get(redisKey);
        if (stored == null) {
            throw new InvalidCredentialsException(); // expired, already used, or forged
        }

        // Single-use: invalidate immediately, then issue a fresh pair (rotation).
        redisTemplate.delete(redisKey);

        String[] parts = stored.split("::", 2);
        UUID userId = UUID.fromString(parts[0]);
        String username = parts[1];
        return issueTokenPair(userId, username);
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String token) {
        // Store a hash, not the raw token, so a Redis dump/backup doesn't itself hand out valid tokens.
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
