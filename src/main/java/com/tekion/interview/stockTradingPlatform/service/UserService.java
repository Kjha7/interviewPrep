package com.tekion.interview.stockTradingPlatform.service;

import com.tekion.interview.stockTradingPlatform.dto.request.LoginRequest;
import com.tekion.interview.stockTradingPlatform.dto.request.RegisterUserRequest;
import com.tekion.interview.stockTradingPlatform.dto.response.LoginResponse;
import com.tekion.interview.stockTradingPlatform.dto.response.RegisterUserResponse;

public interface UserService {
    RegisterUserResponse register(RegisterUserRequest request);
    LoginResponse login(LoginRequest request);
}
