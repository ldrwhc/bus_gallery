package com.busgallery.groupbuy.domain.model;

import lombok.Builder;
import lombok.Data;

/**
 * Goods aggregate used by group-buy domain.
 */
@Data
@Builder
public class GoodsAggregate {
    private String goodsId;
    private String title;
    private long originalPriceCents;
    private long groupPriceCents;
    private int stockAvailable;
    private boolean enabled;
}
