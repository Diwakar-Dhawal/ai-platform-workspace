package com.aiservice.platform.identity.exception;

public class ValidationException extends ApiException {

    public ValidationException(String message) {
        super(
                "VALIDATION_FAILED",
                message
        );
    }
}