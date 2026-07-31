package com.aiservice.platform.identity.dto.response;



import com.aiservice.platform.identity.enums.ResponseStatus;

import java.util.List;

public record ApiResponse<T>(

        ResponseStatus status,

        String message,

        T data,

        ErrorResponse error,

        List<WarningResponse> warnings

) {}