package com.busgallery.groupbuy.domain.service;

import com.busgallery.groupbuy.domain.model.ActivityAggregate;
import com.busgallery.groupbuy.domain.model.GoodsAggregate;
import com.busgallery.groupbuy.domain.model.LockOrderProjection;
import com.busgallery.groupbuy.domain.model.RefundOrderProjection;
import com.busgallery.groupbuy.domain.model.SettleOrderProjection;
import com.busgallery.groupbuy.domain.model.TeamOrderAggregate;
import com.busgallery.groupbuy.domain.model.UserOrderAggregate;
import com.busgallery.groupbuy.domain.port.ActivityRepositoryPort;
import com.busgallery.groupbuy.domain.port.GoodsRepositoryPort;
import com.busgallery.groupbuy.domain.port.TradeCachePort;
import com.busgallery.groupbuy.domain.port.TradeEventPublisherPort;
import com.busgallery.groupbuy.domain.port.TeamOrderRepositoryPort;
import com.busgallery.groupbuy.domain.port.UserOrderRepositoryPort;
import com.busgallery.groupbuy.types.enums.GroupErrorCode;
import com.busgallery.groupbuy.types.exception.GroupBizException;
import com.busgallery.groupbuy.types.model.IdFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Domain service for group-buy trade lifecycle.
 */
@Service
@RequiredArgsConstructor
public class TradeDomainService {
    private static final Duration OUT_TRADE_LOCK_TTL = Duration.ofSeconds(8);
    private static final Duration ORDER_CACHE_TTL = Duration.ofMinutes(10);

    private final GoodsRepositoryPort goodsRepositoryPort;
    private final ActivityRepositoryPort activityRepositoryPort;
    private final TeamOrderRepositoryPort teamOrderRepositoryPort;
    private final UserOrderRepositoryPort userOrderRepositoryPort;
    private final TradeCachePort tradeCachePort;
    private final TradeEventPublisherPort tradeEventPublisherPort;

    /**
     * Lock one order and occupy one team seat.
     *
     * @param userId     user id
     * @param teamId     optional existing team id
     * @param activityId activity id
     * @param goodsId    goods id
     * @param source     source
     * @param channel    channel
     * @param outTradeNo external trade no
     * @return lock result
     */
    @Transactional
    public LockOrderProjection lockOrder(Long userId,
                                         String teamId,
                                         Long activityId,
                                         String goodsId,
                                         String source,
                                         String channel,
                                         String outTradeNo,
                                         Integer orderMode) {
        if (userId == null) {
            throw new GroupBizException(GroupErrorCode.INVALID_PARAM, "userId is required");
        }
        boolean lockAcquired = tradeCachePort.tryLockOutTradeNo(outTradeNo, OUT_TRADE_LOCK_TTL);
        if (!lockAcquired) {
            throw new GroupBizException(GroupErrorCode.CONFLICT, "same outTradeNo is processing");
        }
        try {
        GoodsAggregate goods = goodsRepositoryPort.findByGoodsId(goodsId);
        if (goods == null || !goods.isEnabled()) {
            throw new GroupBizException(GroupErrorCode.NOT_FOUND, "goods is not available");
        }
        ActivityAggregate activity = activityRepositoryPort.findByActivityId(activityId);
        if (activity == null || !activity.isEnabled()) {
            throw new GroupBizException(GroupErrorCode.NOT_FOUND, "activity is not available");
        }
        if (!goodsId.equals(activity.getGoodsId())) {
            throw new GroupBizException(GroupErrorCode.INVALID_PARAM, "activity and goods mismatch");
        }

        UserOrderAggregate existing = tradeCachePort.getCachedOrder(outTradeNo);
        if (existing == null) {
            existing = userOrderRepositoryPort.findByOutTradeNo(outTradeNo);
        }
        if (existing != null) {
            tradeCachePort.cacheOrder(existing, ORDER_CACHE_TTL);
            return LockOrderProjection.builder()
                    .orderId(existing.getOrderId())
                    .teamId(existing.getTeamId())
                    .originalPriceCents(existing.getOriginalPriceCents())
                    .deductionPriceCents(existing.getDeductionPriceCents())
                    .payPriceCents(existing.getPayPriceCents())
                    .orderStatus(existing.getStatus())
                    .build();
        }

        int normalizedOrderMode = normalizeOrderMode(orderMode);
        TeamOrderAggregate team = teamId == null || teamId.isBlank()
                ? null
                : teamOrderRepositoryPort.findByTeamId(teamId);
        if (team == null) {
            long defaultOriginal = goods.getOriginalPriceCents();
            long defaultPay = normalizedOrderMode == 2 ? defaultOriginal : goods.getGroupPriceCents();
            long defaultDeduction = Math.max(0, defaultOriginal - defaultPay);
            team = TeamOrderAggregate.builder()
                    .teamId(IdFactory.nextTeamId())
                    .activityId(activity.getActivityId())
                    .goodsId(goods.getGoodsId())
                    .ownerUserId(userId)
                    .originalPriceCents(defaultOriginal)
                    .deductionPriceCents(defaultDeduction)
                    .payPriceCents(defaultPay)
                    .targetCount(normalizedOrderMode == 2 ? 1 : activity.getTargetCount())
                    .lockCount(0)
                    .completeCount(0)
                    .status("GROUPING")
                    .validStartTime(LocalDateTime.now())
                    .validEndTime(LocalDateTime.now().plusMinutes(activity.getValidMinutes()))
                    .build();
        }

        if (team.getLockCount() >= team.getTargetCount()) {
            throw new GroupBizException(GroupErrorCode.CONFLICT, "team lock target reached");
        }

        long original = goods.getOriginalPriceCents();
        long pay = normalizedOrderMode == 2 ? original : goods.getGroupPriceCents();
        long deduction = Math.max(0, original - pay);

        UserOrderAggregate order = UserOrderAggregate.builder()
                .orderId(IdFactory.nextOrderId())
                .teamId(team.getTeamId())
                .activityId(activityId)
                .goodsId(goodsId)
                .userId(userId)
                .outTradeNo(outTradeNo)
                .source(source)
                .channel(channel)
                .orderMode(normalizedOrderMode)
                .originalPriceCents(original)
                .deductionPriceCents(deduction)
                .payPriceCents(pay)
                .status("LOCKED")
                .createdAt(LocalDateTime.now())
                .build();
        userOrderRepositoryPort.save(order);
        tradeCachePort.cacheOrder(order, ORDER_CACHE_TTL);

        team.setLockCount(team.getLockCount() + 1);
        teamOrderRepositoryPort.save(team);
        tradeEventPublisherPort.publishOrderLocked(order);

        return LockOrderProjection.builder()
                .orderId(order.getOrderId())
                .teamId(order.getTeamId())
                .originalPriceCents(order.getOriginalPriceCents())
                .deductionPriceCents(order.getDeductionPriceCents())
                .payPriceCents(order.getPayPriceCents())
                .orderStatus(order.getStatus())
                .build();
        } finally {
            tradeCachePort.unlockOutTradeNo(outTradeNo);
        }
    }

    private int normalizeOrderMode(Integer orderMode) {
        return orderMode != null && orderMode == 2 ? 2 : 1;
    }

    /**
     * Settle one order after payment.
     *
     * @param userId       user id
     * @param outTradeNo   external trade no
     * @param outTradeTime payment time
     * @return settlement result
     */
    @Transactional
    public SettleOrderProjection settleOrder(Long userId, String outTradeNo, LocalDateTime outTradeTime) {
        if (userId == null) {
            throw new GroupBizException(GroupErrorCode.INVALID_PARAM, "userId is required");
        }
        UserOrderAggregate order = tradeCachePort.getCachedOrder(outTradeNo);
        if (order == null) {
            order = userOrderRepositoryPort.findByOutTradeNo(outTradeNo);
        }
        if (order == null) {
            throw new GroupBizException(GroupErrorCode.NOT_FOUND, "order not found");
        }
        if (!order.getUserId().equals(userId)) {
            throw new GroupBizException(GroupErrorCode.CONFLICT, "order user mismatch");
        }
        if ("PAID".equals(order.getStatus())) {
            return buildSettleProjection(order);
        }

        order.setStatus("PAID");
        order.setOutTradeTime(outTradeTime);
        userOrderRepositoryPort.save(order);
        tradeCachePort.cacheOrder(order, ORDER_CACHE_TTL);

        TeamOrderAggregate team = teamOrderRepositoryPort.findByTeamId(order.getTeamId());
        if (team != null) {
            team.setCompleteCount(team.getCompleteCount() + 1);
            if (team.getCompleteCount() >= team.getTargetCount()) {
                team.setStatus("SUCCESS");
            }
            teamOrderRepositoryPort.save(team);
        }
        tradeEventPublisherPort.publishOrderSettled(order);

        return buildSettleProjection(order);
    }

    /**
     * Refund one order.
     *
     * @param userId     user id
     * @param outTradeNo external trade no
     * @return refund result
     */
    @Transactional
    public RefundOrderProjection refundOrder(Long userId, String outTradeNo) {
        if (userId == null) {
            throw new GroupBizException(GroupErrorCode.INVALID_PARAM, "userId is required");
        }
        UserOrderAggregate order = tradeCachePort.getCachedOrder(outTradeNo);
        if (order == null) {
            order = userOrderRepositoryPort.findByOutTradeNo(outTradeNo);
        }
        if (order == null) {
            throw new GroupBizException(GroupErrorCode.NOT_FOUND, "order not found");
        }
        if (!order.getUserId().equals(userId)) {
            throw new GroupBizException(GroupErrorCode.CONFLICT, "order user mismatch");
        }
        if ("REFUNDED".equals(order.getStatus())) {
            return buildRefundProjection(order);
        }

        String previousStatus = order.getStatus();
        order.setStatus("REFUNDED");
        userOrderRepositoryPort.save(order);
        tradeCachePort.cacheOrder(order, ORDER_CACHE_TTL);

        TeamOrderAggregate team = teamOrderRepositoryPort.findByTeamId(order.getTeamId());
        if (team != null) {
            if (team.getLockCount() > 0) {
                team.setLockCount(team.getLockCount() - 1);
            }
            if ("PAID".equals(previousStatus) && team.getCompleteCount() > 0) {
                team.setCompleteCount(team.getCompleteCount() - 1);
            }
            if ("SUCCESS".equals(team.getStatus()) && team.getCompleteCount() < team.getTargetCount()) {
                team.setStatus("GROUPING");
            }
            teamOrderRepositoryPort.save(team);
        }
        tradeEventPublisherPort.publishOrderRefunded(order);

        return buildRefundProjection(order);
    }

    private SettleOrderProjection buildSettleProjection(UserOrderAggregate order) {
        return SettleOrderProjection.builder()
                .userId(String.valueOf(order.getUserId()))
                .orderId(order.getOrderId())
                .teamId(order.getTeamId())
                .activityId(order.getActivityId())
                .outTradeNo(order.getOutTradeNo())
                .orderStatus(order.getStatus())
                .build();
    }

    private RefundOrderProjection buildRefundProjection(UserOrderAggregate order) {
        return RefundOrderProjection.builder()
                .userId(String.valueOf(order.getUserId()))
                .orderId(order.getOrderId())
                .teamId(order.getTeamId())
                .code("REFUND_SUCCESS")
                .message("refund processed")
                .build();
    }
}
