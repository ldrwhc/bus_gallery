package com.busgallery.groupbuy.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * User-facing trade message.
 */
@Data
@Builder
public class PortalUserMessageResponse {
    private String messageId;
    private String messageType;
    private String title;
    private String content;
    private String bizRecordId;
    private LocalDateTime createdAt;
}

