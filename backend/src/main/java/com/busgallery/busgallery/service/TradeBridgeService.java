package com.busgallery.busgallery.service;

import com.busgallery.busgallery.dto.response.TradeBindingResponse;

/**
 * Bridge service between content image domain and trade_center domain.
 */
public interface TradeBridgeService {

    /**
     * Resolve or create one goods/activity binding by image id.
     *
     * @param imageId           content image id
     * @param preferredVehicleId optional vehicle id hint
     * @return binding snapshot
     */
    TradeBindingResponse resolveOrCreateByImageId(Long imageId, Long preferredVehicleId);

    /**
     * Query one existing goods/activity binding by goods id.
     *
     * @param goodsId trade goods id
     * @return binding snapshot
     */
    TradeBindingResponse queryByGoodsId(String goodsId);
}
