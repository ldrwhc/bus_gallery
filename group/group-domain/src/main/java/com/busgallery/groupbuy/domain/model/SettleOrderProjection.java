package com.busgallery.groupbuy.domain.model;

import lombok.Builder;
import lombok.Data;

/**
 * Domain projection for settlement result.
 */
@Data
@Builder
public class SettleOrderProjection {
    private String userId;
    private String orderId;
    private String teamId;
    private Long activityId;
    private String outTradeNo;
    private String orderStatus;
}
