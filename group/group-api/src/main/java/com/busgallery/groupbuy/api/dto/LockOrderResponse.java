package com.busgallery.groupbuy.api.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Response object for locking order.
 */
@Data
@Builder
public class LockOrderResponse {
    private String orderId;
    private String teamId;
    private long originalPriceCents;
    private long deductionPriceCents;
    private long payPriceCents;
    private String orderStatus;
}
