package com.busgallery.groupbuy.types.exception;

import com.busgallery.groupbuy.types.enums.GroupErrorCode;
import lombok.Getter;

/**
 * Domain-oriented runtime exception with explicit error code.
 */
@Getter
public class GroupBizException extends RuntimeException {
    private final GroupErrorCode errorCode;

    public GroupBizException(GroupErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
