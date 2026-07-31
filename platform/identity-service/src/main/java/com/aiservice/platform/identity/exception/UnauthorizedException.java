package com.aiservice.platform.identity.exception;

public class UnauthorizedException extends ApiException {

    public UnauthorizedException() {
        super(
                "UNAUTHORIZED",
                "Authentication is required."
        );
    }
}