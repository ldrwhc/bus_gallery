package com.busgallery.groupbuy.trigger.http;

import com.busgallery.groupbuy.types.enums.GroupErrorCode;
import com.busgallery.groupbuy.types.exception.GroupBizException;
import com.busgallery.groupbuy.types.model.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for trigger layer.
 */
@RestControllerAdvice
public class GlobalApiExceptionHandler {

    /**
     * Handle business exception.
     *
     * @param ex exception
     * @return normalized error response
     */
    @ExceptionHandler(GroupBizException.class)
    public ApiResponse<Void> handleBizException(GroupBizException ex) {
        return ApiResponse.failure(ex.getErrorCode(), ex.getMessage());
    }

    /**
     * Handle validation exception for request body.
     *
     * @param ex exception
     * @return normalized error response
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("invalid request");
        return ApiResponse.failure(GroupErrorCode.INVALID_PARAM, message);
    }

    /**
     * Handle constraint violation for query/path validation.
     *
     * @param ex exception
     * @return normalized error response
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Void> handleConstraintViolationException(ConstraintViolationException ex) {
        return ApiResponse.failure(GroupErrorCode.INVALID_PARAM, ex.getMessage());
    }

    /**
     * Handle uncaught exception.
     *
     * @param ex exception
     * @return normalized error response
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception ex) {
        return ApiResponse.failure(GroupErrorCode.INTERNAL_ERROR, ex.getMessage());
    }
}
