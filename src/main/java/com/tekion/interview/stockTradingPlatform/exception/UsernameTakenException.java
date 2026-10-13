package com.tekion.interview.stockTradingPlatform.exception;

import org.springframework.http.HttpStatus;

public class UsernameTakenException extends ApiException {
    public UsernameTakenException(String username) {
        super(HttpStatus.CONFLICT, "USERNAME_TAKEN", "Username already exists: " + username);
    }
}
