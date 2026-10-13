package com.tekion.interview.stockTradingPlatform.controller;

import com.tekion.interview.stockTradingPlatform.dto.response.PortfolioResponse;
import com.tekion.interview.stockTradingPlatform.exception.ApiException;
import com.tekion.interview.stockTradingPlatform.security.AuthTokenFilter;
import com.tekion.interview.stockTradingPlatform.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final PortfolioService portfolioService;

    @GetMapping("/{userId}/portfolio")
    public ResponseEntity<PortfolioResponse> getPortfolio(
            @PathVariable UUID userId,
            @RequestParam(required = false) String range,
            @RequestAttribute(AuthTokenFilter.AUTHENTICATED_USER_ID_ATTR) UUID authenticatedUserId) {

        // IDOR guard - see design-doc feedback #5: the path userId must match the token's
        // own identity, or any authenticated user could read anyone else's portfolio by
        // swapping the path parameter.
        if (!authenticatedUserId.equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Cannot access another user's portfolio");
        }

        return ResponseEntity.ok(portfolioService.getPortfolio(userId, range));
    }
}
