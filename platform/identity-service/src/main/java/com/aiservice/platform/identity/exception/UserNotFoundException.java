package com.aiservice.platform.identity.exception;

public class UserNotFoundException extends ApiException {

    public UserNotFoundException() {
        super(
                "USER_NOT_FOUND",
                "User does not exist."
        );
    }
}