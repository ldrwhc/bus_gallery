package com.busgallery.groupbuy.api.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Settlement response object.
 */
@Data
@Builder
public class SettleOrderResponse {
    private String userId;
    private String orderId;
    private String teamId;
    private Long activityId;
    private String outTradeNo;
    private String orderStatus;
}
