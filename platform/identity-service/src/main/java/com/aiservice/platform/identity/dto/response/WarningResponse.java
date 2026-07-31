package com.aiservice.platform.identity.dto.response;

import com.aiservice.platform.identity.enums.ErrorCode;


public record WarningResponse<T>(
        ErrorCode code,
        String message
) {}