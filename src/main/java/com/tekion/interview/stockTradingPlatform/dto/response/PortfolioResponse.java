package com.tekion.interview.stockTradingPlatform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioResponse {
    private UUID userId;
    private String range; // echoes the requested filter, e.g. "3M" - see note in OrderController javadoc
    private List<HoldingDto> holdings;
    private Instant asOf;
}
