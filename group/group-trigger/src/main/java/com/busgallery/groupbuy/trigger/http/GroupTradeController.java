package com.busgallery.groupbuy.trigger.http;

import com.busgallery.groupbuy.api.dto.LockOrderRequest;
import com.busgallery.groupbuy.api.dto.LockOrderResponse;
import com.busgallery.groupbuy.api.dto.RefundOrderRequest;
import com.busgallery.groupbuy.api.dto.RefundOrderResponse;
import com.busgallery.groupbuy.api.dto.SettleOrderRequest;
import com.busgallery.groupbuy.api.dto.SettleOrderResponse;
import com.busgallery.groupbuy.api.service.GroupTradeApi;
import com.busgallery.groupbuy.domain.model.LockOrderProjection;
import com.busgallery.groupbuy.domain.model.SettleOrderProjection;
import com.busgallery.groupbuy.domain.service.TradeDomainService;
import com.busgallery.groupbuy.trigger.auth.AuthContextHolder;
import com.busgallery.groupbuy.trigger.auth.RequireLogin;
import com.busgallery.groupbuy.trigger.service.TradePortalService;
import com.busgallery.groupbuy.types.model.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Trigger layer controller for trade command APIs.
 */
@RestController
@RequestMapping("/api/v1/group/trade")
@RequireLogin
@RequiredArgsConstructor
public class GroupTradeController implements GroupTradeApi {
    private final TradeDomainService tradeDomainService;
    private final TradePortalService tradePortalService;

    /**
     * Lock one order.
     *
     * @param request request body
     * @return lock result
     */
    @Override
    @PostMapping("/lock")
    public ApiResponse<LockOrderResponse> lockOrder(@Valid @RequestBody LockOrderRequest request) {
        Long userId = AuthContextHolder.requirePrincipal().getUserId();
        LockOrderProjection projection = tradeDomainService.lockOrder(
                userId,
                request.getTeamId(),
                request.getActivityId(),
                request.getGoodsId(),
                request.getSource(),
                request.getChannel(),
                request.getOutTradeNo(),
                request.getOrderMode()
        );
        LockOrderResponse response = LockOrderResponse.builder()
                .orderId(projection.getOrderId())
                .teamId(projection.getTeamId())
                .originalPriceCents(projection.getOriginalPriceCents())
                .deductionPriceCents(projection.getDeductionPriceCents())
                .payPriceCents(projection.getPayPriceCents())
                .orderStatus(projection.getOrderStatus())
                .build();
        return ApiResponse.success(response);
    }

    /**
     * Settle one order.
     *
     * @param request request body
     * @return settlement result
     */
    @Override
    @PostMapping("/settle")
    public ApiResponse<SettleOrderResponse> settleOrder(@Valid @RequestBody SettleOrderRequest request) {
        Long userId = AuthContextHolder.requirePrincipal().getUserId();
        SettleOrderProjection projection = tradeDomainService.settleOrder(
                userId,
                request.getOutTradeNo(),
                request.getOutTradeTime()
        );
        SettleOrderResponse response = SettleOrderResponse.builder()
                .userId(projection.getUserId())
                .orderId(projection.getOrderId())
                .teamId(projection.getTeamId())
                .activityId(projection.getActivityId())
                .outTradeNo(projection.getOutTradeNo())
                .orderStatus(projection.getOrderStatus())
                .build();
        return ApiResponse.success(response);
    }

    /**
     * Refund one order.
     *
     * @param request request body
     * @return refund result
     */
    @Override
    @PostMapping("/refund")
    public ApiResponse<RefundOrderResponse> refundOrder(@Valid @RequestBody RefundOrderRequest request) {
        Long userId = AuthContextHolder.requirePrincipal().getUserId();
        RefundOrderResponse response = tradePortalService.refundAndSyncRecord(userId, request.getOutTradeNo());
        return ApiResponse.success(response);
    }
}
