package com.aiservice.platform.identity.exception;

import com.aiservice.platform.identity.enums.ErrorCode;

public class DuplicateResourceException extends ApiException {

    public DuplicateResourceException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}