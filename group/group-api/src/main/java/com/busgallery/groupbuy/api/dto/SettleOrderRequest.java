package com.busgallery.groupbuy.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Request object for settlement.
 */
@Data
public class SettleOrderRequest {
    @NotBlank(message = "source is required")
    private String source;

    @NotBlank(message = "channel is required")
    private String channel;

    @NotBlank(message = "outTradeNo is required")
    private String outTradeNo;

    @NotNull(message = "outTradeTime is required")
    private LocalDateTime outTradeTime;
}
