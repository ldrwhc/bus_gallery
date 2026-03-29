package com.busgallery.groupbuy.domain.port;

import com.busgallery.groupbuy.domain.model.UserOrderAggregate;

/**
 * Domain event publisher port for trade lifecycle events.
 */
public interface TradeEventPublisherPort {

    /**
     * Publish order locked event.
     *
     * @param order order aggregate
     */
    void publishOrderLocked(UserOrderAggregate order);

    /**
     * Publish order settled event.
     *
     * @param order order aggregate
     */
    void publishOrderSettled(UserOrderAggregate order);

    /**
     * Publish order refunded event.
     *
     * @param order order aggregate
     */
    void publishOrderRefunded(UserOrderAggregate order);
}
