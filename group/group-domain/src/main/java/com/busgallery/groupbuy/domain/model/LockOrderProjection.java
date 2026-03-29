package com.busgallery.groupbuy.domain.model;

import lombok.Builder;
import lombok.Data;

/**
 * Domain projection for lock order result.
 */
@Data
@Builder
public class LockOrderProjection {
    private String orderId;
    private String teamId;
    private long originalPriceCents;
    private long deductionPriceCents;
    private long payPriceCents;
    private String orderStatus;
}
