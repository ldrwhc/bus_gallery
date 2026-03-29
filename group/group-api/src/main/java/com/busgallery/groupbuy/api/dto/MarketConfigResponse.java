package com.busgallery.groupbuy.api.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Market config returned by index API.
 */
@Data
@Builder
public class MarketConfigResponse {
    private String goodsId;
    private Long activityId;
    private String goodsTitle;
    private long originalPriceCents;
    private long groupPriceCents;
    private int targetCount;
    private int validMinutes;
    private int activeTeamCount;
}
