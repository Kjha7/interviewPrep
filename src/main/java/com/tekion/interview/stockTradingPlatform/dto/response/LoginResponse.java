package com.tekion.interview.stockTradingPlatform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private UUID userId;
    private String sessionToken;        // JWT, stateless, ~15 min
    private long sessionTokenExpiresIn;  // seconds
    private String refreshToken;        // opaque, stored hashed in Redis, ~8h, single-use/rotated
    private long refreshTokenExpiresIn;  // seconds
}
