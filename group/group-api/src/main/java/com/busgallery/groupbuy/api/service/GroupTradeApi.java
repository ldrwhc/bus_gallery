package com.busgallery.groupbuy.api.service;

import com.busgallery.groupbuy.api.dto.LockOrderRequest;
import com.busgallery.groupbuy.api.dto.LockOrderResponse;
import com.busgallery.groupbuy.api.dto.RefundOrderRequest;
import com.busgallery.groupbuy.api.dto.RefundOrderResponse;
import com.busgallery.groupbuy.api.dto.SettleOrderRequest;
import com.busgallery.groupbuy.api.dto.SettleOrderResponse;
import com.busgallery.groupbuy.types.model.ApiResponse;

/**
 * Public API definition for group-buy trade operations.
 */
public interface GroupTradeApi {

    /**
     * Lock order and occupy team slot.
     *
     * @param request request
     * @return lock result
     */
    ApiResponse<LockOrderResponse> lockOrder(LockOrderRequest request);

    /**
     * Settle one locked order after payment callback.
     *
     * @param request request
     * @return settlement result
     */
    ApiResponse<SettleOrderResponse> settleOrder(SettleOrderRequest request);

    /**
     * Refund one order.
     *
     * @param request request
     * @return refund result
     */
    ApiResponse<RefundOrderResponse> refundOrder(RefundOrderRequest request);
}
