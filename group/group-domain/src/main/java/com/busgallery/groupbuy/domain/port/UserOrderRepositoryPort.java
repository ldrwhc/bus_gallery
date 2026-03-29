package com.busgallery.groupbuy.domain.port;

import com.busgallery.groupbuy.domain.model.UserOrderAggregate;

/**
 * Repository port for user order aggregate.
 */
public interface UserOrderRepositoryPort {

    /**
     * Find one order by external trade no.
     *
     * @param outTradeNo external trade number
     * @return user order aggregate
     */
    UserOrderAggregate findByOutTradeNo(String outTradeNo);

    /**
     * Save user order aggregate.
     *
     * @param aggregate order aggregate
     */
    void save(UserOrderAggregate aggregate);
}
