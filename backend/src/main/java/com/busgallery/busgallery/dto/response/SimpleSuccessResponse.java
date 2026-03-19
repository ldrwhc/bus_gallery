package com.busgallery.busgallery.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SimpleSuccessResponse {
    private boolean success;
    private String message;

    public static SimpleSuccessResponse ok(String message) {
        return SimpleSuccessResponse.builder()
                .success(true)
                .message(message)
                .build();
    }
}

