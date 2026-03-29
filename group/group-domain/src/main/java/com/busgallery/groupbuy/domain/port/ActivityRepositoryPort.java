package com.busgallery.groupbuy.domain.port;

import com.busgallery.groupbuy.domain.model.ActivityAggregate;

/**
 * Repository port for activity aggregate.
 */
public interface ActivityRepositoryPort {

    /**
     * Find one activity by id.
     *
     * @param activityId activity id
     * @return activity aggregate
     */
    ActivityAggregate findByActivityId(Long activityId);

    /**
     * Find current active activity by goods id.
     *
     * @param goodsId goods id
     * @return activity aggregate
     */
    ActivityAggregate findActiveByGoodsId(String goodsId);

    /**
     * Count active teams under one activity.
     *
     * @param activityId activity id
     * @return team count
     */
    int countActiveTeams(Long activityId);
}
