package com.busgallery.groupbuy.domain.service;

import com.busgallery.groupbuy.domain.model.ActivityAggregate;
import com.busgallery.groupbuy.domain.model.GoodsAggregate;
import com.busgallery.groupbuy.domain.model.MarketConfigProjection;
import com.busgallery.groupbuy.domain.port.ActivityRepositoryPort;
import com.busgallery.groupbuy.domain.port.GoodsRepositoryPort;
import com.busgallery.groupbuy.types.enums.GroupErrorCode;
import com.busgallery.groupbuy.types.exception.GroupBizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Domain service for market read operations.
 */
@Service
@RequiredArgsConstructor
public class MarketDomainService {
    private final GoodsRepositoryPort goodsRepositoryPort;
    private final ActivityRepositoryPort activityRepositoryPort;

    /**
     * Build market config for one goods and activity.
     *
     * @param goodsId goods id
     * @return market config
     */
    public MarketConfigProjection queryMarketConfig(String goodsId) {
        GoodsAggregate goods = goodsRepositoryPort.findByGoodsId(goodsId);
        if (goods == null || !goods.isEnabled()) {
            throw new GroupBizException(GroupErrorCode.NOT_FOUND, "goods is not available");
        }

        ActivityAggregate activity = activityRepositoryPort.findActiveByGoodsId(goodsId);
        if (activity == null || !activity.isEnabled()) {
            throw new GroupBizException(GroupErrorCode.NOT_FOUND, "activity is not available");
        }

        int activeTeamCount = activityRepositoryPort.countActiveTeams(activity.getActivityId());
        return MarketConfigProjection.builder()
                .goodsId(goods.getGoodsId())
                .activityId(activity.getActivityId())
                .goodsTitle(goods.getTitle())
                .originalPriceCents(goods.getOriginalPriceCents())
                .groupPriceCents(goods.getGroupPriceCents())
                .targetCount(activity.getTargetCount())
                .validMinutes(activity.getValidMinutes())
                .activeTeamCount(activeTeamCount)
                .build();
    }
}
