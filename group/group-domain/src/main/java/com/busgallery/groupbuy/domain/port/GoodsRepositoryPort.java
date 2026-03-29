package com.busgallery.groupbuy.domain.port;

import com.busgallery.groupbuy.domain.model.GoodsAggregate;

/**
 * Repository port for goods aggregate.
 */
public interface GoodsRepositoryPort {

    /**
     * Find goods by goods id.
     *
     * @param goodsId goods id
     * @return goods aggregate or null
     */
    GoodsAggregate findByGoodsId(String goodsId);
}
