package com.tekion.interview.stockTradingPlatform.dto.response;

import com.tekion.interview.stockTradingPlatform.entity.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterUserResponse {
    private UUID userId;
    private String username;
    private UserStatus status;
    private Instant createdAt;
}
