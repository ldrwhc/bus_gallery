package com.busgallery.busgallery.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Unified error codes aligned with HTTP-style semantics.
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    SUCCESS("00000", "success"),
    INVALID_PARAM("A0400", "Invalid request parameter"),
    UNAUTHORIZED("A0401", "Unauthorized or session expired"),
    NOT_FOUND("A0404", "Resource not found"),
    REQUEST_DUPLICATE("A0429", "Duplicate request, please do not resubmit"),
    BUSINESS_ERROR("A0500", "Business error"),
    INTERNAL_ERROR("B0500", "Internal server error"),
    STORAGE_ERROR("B0501", "Storage service error");

    private final String code;
    private final String message;
}
