package com.busgallery.groupbuy.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * User-facing trade record.
 */
@Data
@Builder
public class PortalUserRecordResponse {
    private String recordId;
    private String orderId;
    private String outTradeNo;
    private String teamId;
    private String goodsId;
    private Long imageId;
    private Long vehicleId;
    private String title;
    private String coverUrl;
    private int orderMode;
    private int tradeStatus;
    private long payPriceCents;
    private boolean canDownload;
    private String downloadUrl;
    private LocalDateTime createdAt;
}

