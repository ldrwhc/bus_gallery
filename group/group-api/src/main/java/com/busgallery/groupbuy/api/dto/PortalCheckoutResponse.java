package com.busgallery.groupbuy.api.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Response for portal checkout.
 */
@Data
@Builder
public class PortalCheckoutResponse {
    private String outTradeNo;
    private String orderId;
    private String teamId;
    private String orderStatus;
    private String teamStatus;
    private long payPriceCents;
    private String recordId;
    private boolean canDownload;
    private boolean waitingGroup;
    private String message;
}

