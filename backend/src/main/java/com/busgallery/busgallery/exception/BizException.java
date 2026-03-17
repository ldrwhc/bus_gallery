package com.busgallery.busgallery.exception;

/**
 * BizException类用于封装BizException相关的领域职责（所在包：com.busgallery.busgallery.exception）。
 */
public class BizException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * BizException构造器用于初始化对象状态。
     * @param errorCode errorCode参数，详见调用方上下文。
     * @return 构造器无返回值。
     */
    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * BizException构造器用于初始化对象状态。
     * @param errorCode errorCode参数，详见调用方上下文。
     * @param message message参数，详见调用方上下文。
     * @return 构造器无返回值。
     */
    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * getErrorCode方法用于处理getErrorCode相关的业务逻辑。
     * @return 返回ErrorCode类型结果。
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}