package com.busgallery.groupbuy.api.service;

import com.busgallery.groupbuy.api.dto.MarketConfigRequest;
import com.busgallery.groupbuy.api.dto.MarketConfigResponse;
import com.busgallery.groupbuy.types.model.ApiResponse;

/**
 * Public API definition for market read operations.
 */
public interface GroupMarketApi {

    /**
     * Query market config for one goods.
     *
     * @param request request object
     * @return market config response
     */
    ApiResponse<MarketConfigResponse> queryMarketConfig(MarketConfigRequest request);
}
