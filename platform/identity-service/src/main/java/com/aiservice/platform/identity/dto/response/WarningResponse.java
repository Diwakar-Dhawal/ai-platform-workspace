package com.aiservice.platform.identity.dto.response;

import com.aiservice.platform.identity.enums.ErrorCode;
import com.aiservice.platform.identity.enums.WarningCode;


public record WarningResponse(
        WarningCode code,
        String field,
        String message
) {}