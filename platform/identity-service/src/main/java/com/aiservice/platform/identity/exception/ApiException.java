package com.aiservice.platform.identity.exception;

import lombok.Getter;

@Getter
public abstract class ApiException extends RuntimeException {

    private final String errorCode;

    protected ApiException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}