package com.busgallery.bridge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bridge service discovery and proxy properties.
 */
@Data
@ConfigurationProperties(prefix = "bridge")
public class BridgeProperties {
    /**
     * Content service name in discovery registry.
     */
    private String contentServiceName = "bus-gallery";

    /**
     * Trade service name in discovery registry.
     */
    private String tradeServiceName = "bus-gallery-group-buy";

    /**
     * Content service base URL used by binary proxying.
     * Default uses logical service name and requires a load-balanced RestTemplate.
     */
    private String contentServiceBaseUrl = "http://bus-gallery";
}
