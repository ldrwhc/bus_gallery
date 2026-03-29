package com.busgallery.groupbuy.domain.model;

import lombok.Builder;
import lombok.Data;

/**
 * Domain projection for refund result.
 */
@Data
@Builder
public class RefundOrderProjection {
    private String userId;
    private String orderId;
    private String teamId;
    private String code;
    private String message;
}
