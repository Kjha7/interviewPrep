package com.tekion.interview.stockTradingPlatform.service.impl;

import com.tekion.interview.stockTradingPlatform.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Key layout: idem:{key} -> "<sha256(requestBody)>::<status>[::<responseJson>]"
 * status is one of PROCESSING | DONE
 *
 * SET ... NX is the atomic compare-and-set primitive here: exactly one caller wins the
 * race to create the key, everyone else observes it already exists.
 */
@Service
@RequiredArgsConstructor
public class IdempotencyServiceImpl implements IdempotencyService {

    private static final String KEY_PREFIX = "idem:";
    private static final String SEPARATOR = "::";

    private final StringRedisTemplate redisTemplate;

    @Value("${app.idempotency.ttl-hours:24}")
    private long ttlHours;

    @Override
    public Result checkAndReserve(String idempotencyKey, String requestBodyHash) {
        String redisKey = KEY_PREFIX + idempotencyKey;
        String initialValue = requestBodyHash + SEPARATOR + "PROCESSING";

        Boolean wasSet = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, initialValue, Duration.ofHours(ttlHours));

        if (Boolean.TRUE.equals(wasSet)) {
            return new Result(Outcome.PROCEED, null);
        }

        String existing = redisTemplate.opsForValue().get(redisKey);
        if (existing == null) {
            // TTL raced out between the failed SETNX and this GET - safe to just retry as PROCEED.
            return checkAndReserve(idempotencyKey, requestBodyHash);
        }

        String[] parts = existing.split(SEPARATOR, 3);
        String storedHash = parts[0];
        String status = parts[1];

        if (!storedHash.equals(requestBodyHash)) {
            return new Result(Outcome.CONFLICT, null);
        }
        if ("DONE".equals(status)) {
            return new Result(Outcome.REPLAY_COMPLETE, parts.length > 2 ? parts[2] : null);
        }
        return new Result(Outcome.REPLAY_IN_PROGRESS, null);
    }

    @Override
    public void complete(String idempotencyKey, String responseJson) {
        String redisKey = KEY_PREFIX + idempotencyKey;
        String existing = redisTemplate.opsForValue().get(redisKey);
        if (existing == null) return; // TTL expired before completion - acceptable, rare edge case

        String storedHash = existing.split(SEPARATOR, 3)[0];
        String finalValue = storedHash + SEPARATOR + "DONE" + SEPARATOR + responseJson;
        redisTemplate.opsForValue().set(redisKey, finalValue, Duration.ofHours(ttlHours));
    }
}
