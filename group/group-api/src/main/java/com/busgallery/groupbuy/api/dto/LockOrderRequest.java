package com.busgallery.groupbuy.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request object for locking a group-buy order.
 */
@Data
public class LockOrderRequest {
    private String teamId;

    /**
     * 1 = group order, 2 = direct order.
     */
    private Integer orderMode;

    @NotNull(message = "activityId is required")
    private Long activityId;

    @NotBlank(message = "goodsId is required")
    private String goodsId;

    @NotBlank(message = "source is required")
    private String source;

    @NotBlank(message = "channel is required")
    private String channel;

    @NotBlank(message = "outTradeNo is required")
    private String outTradeNo;
}
