package com.busgallery.groupbuy.domain.model;

import lombok.Builder;
import lombok.Data;

/**
 * Domain projection for market config query.
 */
@Data
@Builder
public class MarketConfigProjection {
    private String goodsId;
    private Long activityId;
    private String goodsTitle;
    private long originalPriceCents;
    private long groupPriceCents;
    private int targetCount;
    private int validMinutes;
    private int activeTeamCount;
}
