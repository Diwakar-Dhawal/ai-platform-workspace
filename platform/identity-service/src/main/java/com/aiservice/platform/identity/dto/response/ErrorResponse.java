package com.aiservice.platform.identity.dto.response;

import com.aiservice.platform.identity.enums.ErrorCode;

public record ErrorResponse(

        ErrorCode code,

        String message

) {}