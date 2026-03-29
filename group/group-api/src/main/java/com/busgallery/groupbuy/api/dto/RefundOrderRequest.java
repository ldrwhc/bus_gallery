package com.busgallery.groupbuy.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request object for refund.
 */
@Data
public class RefundOrderRequest {
    @NotBlank(message = "outTradeNo is required")
    private String outTradeNo;

    private String source;

    private String channel;
}
