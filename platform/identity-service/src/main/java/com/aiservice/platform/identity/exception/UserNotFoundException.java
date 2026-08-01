package com.aiservice.platform.identity.exception;

import com.aiservice.platform.identity.enums.ErrorCode;

public class UserNotFoundException extends ApiException {

    public UserNotFoundException(String message) {
        super(ErrorCode.USER_NOT_FOUND, message);
    }
}