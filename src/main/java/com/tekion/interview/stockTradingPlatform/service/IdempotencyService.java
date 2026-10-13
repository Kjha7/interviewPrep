package com.tekion.interview.stockTradingPlatform.service;

public interface IdempotencyService {

    enum Outcome { PROCEED, REPLAY_IN_PROGRESS, REPLAY_COMPLETE, CONFLICT }

    record Result(Outcome outcome, String storedResponseJson) {}

    /**
     * Atomically check-and-reserve an idempotency key.
     * PROCEED       -> caller is the writer, must eventually call complete(...)
     * REPLAY_*       -> caller should return storedResponseJson (or poll) instead of writing
     * CONFLICT      -> same key, different request body -> 409
     */
    Result checkAndReserve(String idempotencyKey, String requestBodyHash);

    void complete(String idempotencyKey, String responseJson);
}
