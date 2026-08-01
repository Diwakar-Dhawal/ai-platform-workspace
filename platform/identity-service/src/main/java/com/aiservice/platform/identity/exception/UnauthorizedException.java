package com.aiservice.platform.identity.exception;

import com.aiservice.platform.identity.enums.ErrorCode;

public class UnauthorizedException extends ApiException {

    public UnauthorizedException(ErrorCode errorCode, String message ) {
        super(errorCode, message);
    }
}