package com.busgallery.groupbuy.types.model;

import com.busgallery.groupbuy.types.enums.GroupErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unified response wrapper for all group-buy APIs.
 *
 * @param <T> response payload type
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private String code;
    private String message;
    private T data;

    /**
     * Build success response.
     *
     * @param data response payload
     * @param <T>  payload type
     * @return success response
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code(GroupErrorCode.SUCCESS.getCode())
                .message(GroupErrorCode.SUCCESS.getMessage())
                .data(data)
                .build();
    }

    /**
     * Build failure response.
     *
     * @param errorCode code enum
     * @param message   override message
     * @param <T>       payload type
     * @return error response
     */
    public static <T> ApiResponse<T> failure(GroupErrorCode errorCode, String message) {
        return ApiResponse.<T>builder()
                .code(errorCode.getCode())
                .message(message == null || message.isBlank() ? errorCode.getMessage() : message)
                .build();
    }
}
