package com.busgallery.busgallery.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Default trade bridge values used when creating goods/activity from content images.
 */
@Data
@Component
@ConfigurationProperties(prefix = "busgallery.trade-bridge")
public class TradeBridgeProperties {

    /**
     * Default original price (cents).
     */
    private long defaultOriginalPriceCents = 19900L;

    /**
     * Default group price (cents).
     */
    private long defaultGroupPriceCents = 14900L;

    /**
     * Default goods stock.
     */
    private int defaultStock = 9999;

    /**
     * Default team target count.
     */
    private int defaultTargetCount = 3;

    /**
     * Default activity valid minutes.
     */
    private int defaultValidMinutes = 1440;

    /**
     * Activity time window extension in days.
     */
    private int defaultActivityDays = 365;
}

