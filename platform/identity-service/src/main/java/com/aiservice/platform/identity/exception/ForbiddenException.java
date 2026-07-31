package com.aiservice.platform.identity.exception;

public class ForbiddenException extends ApiException {

    public ForbiddenException() {
        super(
                "FORBIDDEN",
                "You are not allowed to perform this operation."
        );
    }
}