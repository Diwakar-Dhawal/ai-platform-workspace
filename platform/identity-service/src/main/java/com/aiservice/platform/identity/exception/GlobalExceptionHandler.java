package com.aiservice.platform.identity.exception;

import com.aiservice.platform.identity.dto.response.ApiResponse;
import com.aiservice.platform.identity.dto.response.ErrorResponse;
import com.aiservice.platform.identity.enums.ErrorCode;
import com.aiservice.platform.identity.enums.ResponseStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFound(
            UserNotFoundException ex) {

        return buildResponse(
                HttpStatus.NOT_FOUND,
                ex
        );
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicate(
            DuplicateResourceException ex) {

        return buildResponse(
                HttpStatus.CONFLICT,
                ex
        );
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(
            UnauthorizedException ex) {

        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                ex
        );
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(
            ForbiddenException ex) {

        return buildResponse(
                HttpStatus.FORBIDDEN,
                ex
        );
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            ValidationException ex) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ex
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(
            Exception ex) {

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        new ApiResponse<>(
                                ResponseStatus.FAILED,
                                "Something went wrong.",
                                null,
                                new ErrorResponse(
                                        ErrorCode.INTERNAL_SERVER_ERROR,
                                        ex.getMessage()
                                ),
                                List.of()
                        )
                );
    }

    private ResponseEntity<ApiResponse<Void>> buildResponse(
            HttpStatus status,
            ApiException ex) {

        return ResponseEntity.status(status)
                .body(
                        new ApiResponse<>(
                                ResponseStatus.FAILED,
                                ex.getMessage(),
                                null,
                                new ErrorResponse(
                                       ErrorCode.ACCESS_DENIED,
                                        ex.getMessage()
                                ),
                                List.of()
                        )
                );
    }
}