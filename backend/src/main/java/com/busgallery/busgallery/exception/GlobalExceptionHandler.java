package com.busgallery.busgallery.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;   // 新增
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j                                                 // 新增
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ErrorResponse handleBizException(BizException e) {
        log.warn("业务异常: {}", e.getMessage());     // 记录日志
        return ErrorResponse.of(e.getErrorCode().getCode(), e.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ErrorResponse handleNotFoundException(NotFoundException e) {
        log.warn("资源未找到: {}", e.getMessage());
        return ErrorResponse.of(e.getErrorCode().getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorResponse handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : ErrorCode.INVALID_PARAM.getMessage();
        log.warn("参数校验失败: {}", message);
        return ErrorResponse.of(ErrorCode.INVALID_PARAM.getCode(), message);
    }

    @ExceptionHandler(Exception.class)
    public ErrorResponse handleException(Exception e) {
        log.error("未处理的异常", e);                  // 关键：打印堆栈
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