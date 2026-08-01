package com.aiservice.platform.identity.exception;

import com.aiservice.platform.identity.enums.ErrorCode;

public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }
}