package com.busgallery.busgallery.exception;

public class NotFoundException extends BizException {

    public NotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }

    public NotFoundException() {
        super(ErrorCode.NOT_FOUND, ErrorCode.NOT_FOUND.getMessage());
    }
}