package com.busgallery.groupbuy.api.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Refund response object.
 */
@Data
@Builder
public class RefundOrderResponse {
    private String userId;
    private String orderId;
    private String teamId;
    private String code;
    private String message;
}
