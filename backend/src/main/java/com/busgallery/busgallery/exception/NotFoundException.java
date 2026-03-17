package com.busgallery.busgallery.exception;

/**
 * NotFoundException类用于封装NotFoundException相关的领域职责（所在包：com.busgallery.busgallery.exception）。
 */
public class NotFoundException extends BizException {

    /**
     * NotFoundException构造器用于初始化对象状态。
     * @param message message参数，详见调用方上下文。
     * @return 构造器无返回值。
     */
    public NotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }

    /**
     * NotFoundException构造器用于初始化对象状态。
     * @return 构造器无返回值。
     */
    public NotFoundException() {
        super(ErrorCode.NOT_FOUND, ErrorCode.NOT_FOUND.getMessage());
    }
}