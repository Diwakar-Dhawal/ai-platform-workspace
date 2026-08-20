package com.aiservice.platform.identity.exception;

import com.aiservice.platform.identity.dto.response.ApiResponse;
import com.aiservice.platform.identity.dto.response.ErrorResponse;
import com.aiservice.platform.identity.dto.response.WarningResponse;
import com.aiservice.platform.identity.enums.ErrorCode;
import com.aiservice.platform.identity.enums.ResponseStatus;
import com.aiservice.platform.identity.enums.WarningCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.FieldError;

import org.springframework.security.access.AccessDeniedException;

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

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(
            BadRequestException ex) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ex
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(
                        ApiResponse.failure(
                                "Forbidden",
                                new ErrorResponse(
                                        ErrorCode.FORBIDDEN,
                                        "You do not have permission to access this resource."
                                )
                        )
                );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        ApiResponse.failure(
                                "Internal Server Error",
                                new ErrorResponse(
                                        ErrorCode.INTERNAL_SERVER_ERROR,
                                        "An unexpected error occurred. Please try again later."
                                )
                        )
                );
    }

    private ResponseEntity<ApiResponse<Void>> buildResponse(
            HttpStatus status,
            ApiException ex) {

        return ResponseEntity
                .status(status)
                .body(
                        ApiResponse.failure(
                                ex.getMessage(),
                                new ErrorResponse(
                                        ex.getErrorCode(),
                                        ex.getMessage()
                                )
                        )
                );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex) {

        List<WarningResponse> warnings = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toWarning)
                .toList();

        return ResponseEntity.badRequest()
                .body(
                        new ApiResponse<>(
                                ResponseStatus.FAILED,
                                "Validation failed",
                                null,
                                new ErrorResponse(
                                        ErrorCode.VALIDATION_ERROR,
                                        "One or more request fields are invalid."
                                ),
                                warnings
                        )
                );
    }

    private WarningResponse toWarning(FieldError error) {

        return new WarningResponse(
                WarningCode.VALIDATION_ERROR,
                error.getField(),
                error.getDefaultMessage()
        );
    }
}