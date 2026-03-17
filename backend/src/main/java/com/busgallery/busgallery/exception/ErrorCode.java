package com.busgallery.busgallery.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ErrorCode枚举用于封装ErrorCode相关的领域职责（所在包：com.busgallery.busgallery.exception）。
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    SUCCESS("00000", "成功"),
    INVALID_PARAM("A0400", "请求参数错误"),
    UNAUTHORIZED("A0401", "未登录或登录已过期"),
    NOT_FOUND("A0404", "资源不存在"),
    BUSINESS_ERROR("A0500", "业务异常"),
    INTERNAL_ERROR("B0500", "服务器内部错误");

    private final String code;
    private final String message;
}
