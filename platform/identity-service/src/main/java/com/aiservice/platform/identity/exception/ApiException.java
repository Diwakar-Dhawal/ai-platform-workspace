package com.aiservice.platform.identity.exception;

import com.aiservice.platform.identity.enums.ErrorCode;
import lombok.Getter;

@Getter
public abstract class ApiException extends RuntimeException {

    private final ErrorCode errorCode;

    protected ApiException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}