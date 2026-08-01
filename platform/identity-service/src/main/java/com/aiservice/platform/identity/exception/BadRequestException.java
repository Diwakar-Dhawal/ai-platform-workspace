package com.aiservice.platform.identity.exception;

import com.aiservice.platform.identity.enums.ErrorCode;

public class BadRequestException extends ApiException {
    public BadRequestException(ErrorCode code, String message) {
        super(code, message);
    }
}
