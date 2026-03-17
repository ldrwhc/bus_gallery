package com.busgallery.busgallery.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centralized exception translator producing unified error codes.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ErrorResponse handleBizException(BizException e) {
        log.warn("Business exception: {}", e.getMessage());
        return ErrorResponse.of(e.getErrorCode().getCode(), e.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ErrorResponse handleNotFoundException(NotFoundException e) {
        log.warn("Resource not found: {}", e.getMessage());
        return ErrorResponse.of(e.getErrorCode().getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorResponse handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : ErrorCode.INVALID_PARAM.getMessage();
        log.warn("Request validation failed: {}", message);
        return ErrorResponse.of(ErrorCode.INVALID_PARAM.getCode(), message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ErrorResponse handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Invalid argument: {}", e.getMessage());
        return ErrorResponse.of(ErrorCode.INVALID_PARAM.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ErrorResponse handleException(Exception e) {
        log.error("Unhandled exception", e);
        return ErrorResponse.of(ErrorCode.INTERNAL_ERROR.getCode(), e.getMessage());
    }

    @Data
    @AllArgsConstructor
    public static class ErrorResponse {
        private String code;
        private String message;

        public static ErrorResponse of(String code, String message) {
            return new ErrorResponse(code, message);
        }
    }
}
