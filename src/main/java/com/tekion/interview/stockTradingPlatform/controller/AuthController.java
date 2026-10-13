package com.tekion.interview.stockTradingPlatform.controller;

import com.tekion.interview.stockTradingPlatform.dto.request.LoginRequest;
import com.tekion.interview.stockTradingPlatform.dto.request.RefreshTokenRequest;
import com.tekion.interview.stockTradingPlatform.dto.request.RegisterUserRequest;
import com.tekion.interview.stockTradingPlatform.dto.response.LoginResponse;
import com.tekion.interview.stockTradingPlatform.dto.response.RegisterUserResponse;
import com.tekion.interview.stockTradingPlatform.service.TokenService;
import com.tekion.interview.stockTradingPlatform.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Split out from UserController on purpose - registration/login/refresh are unauthenticated-
 * by-definition endpoints and benefit from being under their own path prefix for gateway
 * rate-limiting rules (auth endpoints get much stricter limits than authenticated reads).
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final TokenService tokenService;

    @PostMapping("/register")
    public ResponseEntity<RegisterUserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(tokenService.refresh(request.getRefreshToken()));
    }
}
