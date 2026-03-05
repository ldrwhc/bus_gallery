package com.busgallery.busgallery.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    SUCCESS("00000", "成功"),
    INVALID_PARAM("A0400", "请求参数错误"),
    NOT_FOUND("A0404", "资源不存在"),
    BUSINESS_ERROR("A0500", "业务异常"),
    INTERNAL_ERROR("B0500", "服务器内部错误");

    private final String code;
    private final String message;
}