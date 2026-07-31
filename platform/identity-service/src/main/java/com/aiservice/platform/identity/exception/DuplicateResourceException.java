package com.aiservice.platform.identity.exception;

public class DuplicateResourceException extends ApiException {

    public DuplicateResourceException(String resource) {
        super(
                "DUPLICATE_RESOURCE",
                resource + " already exists."
        );
    }
}