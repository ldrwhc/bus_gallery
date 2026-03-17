package com.busgallery.busgallery.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;   // 新增
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * GlobalExceptionHandler类用于封装GlobalExceptionHandler相关的领域职责（所在包：com.busgallery.busgallery.exception）。
 */
@Slf4j                                                 // 新增
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * handleBizException方法用于处理handleBizException相关的业务逻辑。
     * @param e e参数，详见调用方上下文。
     * @return 返回ErrorResponse类型结果。
     */
    @ExceptionHandler(BizException.class)
    public ErrorResponse handleBizException(BizException e) {
        log.warn("业务异常: {}", e.getMessage());     // 记录日志
        return ErrorResponse.of(e.getErrorCode().getCode(), e.getMessage());
    }

    /**
     * handleNotFoundException方法用于处理handleNotFoundException相关的业务逻辑。
     * @param e e参数，详见调用方上下文。
     * @return 返回ErrorResponse类型结果。
     */
    @ExceptionHandler(NotFoundException.class)
    public ErrorResponse handleNotFoundException(NotFoundException e) {
        log.warn("资源未找到: {}", e.getMessage());
        return ErrorResponse.of(e.getErrorCode().getCode(), e.getMessage());
    }

    /**
     * handleValidationException方法用于处理handleValidationException相关的业务逻辑。
     * @param e e参数，详见调用方上下文。
     * @return 返回ErrorResponse类型结果。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorResponse handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : ErrorCode.INVALID_PARAM.getMessage();
        log.warn("参数校验失败: {}", message);
        return ErrorResponse.of(ErrorCode.INVALID_PARAM.getCode(), message);
    }

    /**
     * handleException方法用于处理handleException相关的业务逻辑。
     * @param e e参数，详见调用方上下文。
     * @return 返回ErrorResponse类型结果。
     */
    @ExceptionHandler(Exception.class)
    public ErrorResponse handleException(Exception e) {
        log.error("未处理的异常", e);                  // 关键：打印堆栈
        return ErrorResponse.of(ErrorCode.INTERNAL_ERROR.getCode(), e.getMessage());
    }

    /**
     * ErrorResponse类用于封装ErrorResponse相关的领域职责（所在包：com.busgallery.busgallery.exception）。
     */
    @Data
    @AllArgsConstructor
    public static class ErrorResponse {
        private String code;
        private String message;

        /**
         * of方法用于处理of相关的业务逻辑。
         * @param code code参数，详见调用方上下文。
         * @param message message参数，详见调用方上下文。
         * @return 返回ErrorResponse类型结果。
         */
        public static ErrorResponse of(String code, String message) {
            return new ErrorResponse(code, message);
        }
    }
}