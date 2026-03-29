package com.busgallery.groupbuy.domain.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * User order aggregate.
 */
@Data
@Builder
public class UserOrderAggregate {
    private String orderId;
    private String teamId;
    private Long activityId;
    private String goodsId;
    private Long userId;
    private String outTradeNo;
    private String source;
    private String channel;
    private int orderMode;
    private long originalPriceCents;
    private long deductionPriceCents;
    private long payPriceCents;
    private String status;
    private LocalDateTime outTradeTime;
    private LocalDateTime createdAt;
}
