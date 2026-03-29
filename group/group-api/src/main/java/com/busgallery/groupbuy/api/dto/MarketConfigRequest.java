package com.busgallery.groupbuy.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request object for market config query.
 */
@Data
public class MarketConfigRequest {
    @NotBlank(message = "goodsId is required")
    private String goodsId;
}
