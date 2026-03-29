package com.busgallery.groupbuy.types.enums;

import lombok.Getter;

/**
 * Error code catalog for the group-buy service.
 */
@Getter
public enum GroupErrorCode {
    SUCCESS("00000", "ok"),
    UNAUTHORIZED("A0401", "unauthorized"),
    INVALID_PARAM("A0400", "invalid parameter"),
    NOT_FOUND("A0404", "resource not found"),
    CONFLICT("A0409", "resource conflict"),
    INTERNAL_ERROR("B0500", "internal error");

    private final String code;
    private final String message;

    GroupErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
