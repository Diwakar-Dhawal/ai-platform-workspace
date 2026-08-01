package com.aiservice.platform.identity.dto.response;

import com.aiservice.platform.identity.enums.ResponseStatus;

import java.util.List;

public record ApiResponse<T>(

        ResponseStatus status,
        String message,
        T data,
        ErrorResponse error,
        List<WarningResponse> warnings

) {

    public static <T> ApiResponse<T> success(
            String message,
            T data
    ) {
        return new ApiResponse<>(
                ResponseStatus.SUCCESS,
                message,
                data,
                null,
                List.of()
        );
    }

    public static <T> ApiResponse<T> success(
            String message,
            T data,
            List<WarningResponse> warnings
    ) {
        return new ApiResponse<>(
                ResponseStatus.SUCCESS,
                message,
                data,
                null,
                warnings
        );
    }

    public static <T> ApiResponse<T> failure(
            String message,
            ErrorResponse error
    ) {
        return new ApiResponse<>(
                ResponseStatus.FAILED,
                message,
                null,
                error,
                List.of()
        );
    }


}