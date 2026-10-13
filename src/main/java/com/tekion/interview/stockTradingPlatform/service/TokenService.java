package com.tekion.interview.stockTradingPlatform.service;

import com.tekion.interview.stockTradingPlatform.dto.response.LoginResponse;

import java.util.UUID;

public interface TokenService {

    LoginResponse issueTokenPair(UUID userId, String username);

    /** Validates and rotates the refresh token, issuing a fresh pair. Old token is invalidated (single-use). */
    LoginResponse refresh(String refreshToken);
}
