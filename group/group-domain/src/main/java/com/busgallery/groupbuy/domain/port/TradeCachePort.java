package com.busgallery.groupbuy.domain.port;

import com.busgallery.groupbuy.domain.model.UserOrderAggregate;

import java.time.Duration;

/**
 * Cache and idempotency port backed by Redis.
 */
public interface TradeCachePort {

    /**
     * Try acquiring lock for one out-trade-no.
     *
     * @param outTradeNo unique trade number
     * @param ttl        lock ttl
     * @return true if lock acquired
     */
    boolean tryLockOutTradeNo(String outTradeNo, Duration ttl);

    /**
     * Release out-trade-no lock.
     *
     * @param outTradeNo unique trade number
     */
    void unlockOutTradeNo(String outTradeNo);

    /**
     * Get cached order snapshot.
     *
     * @param outTradeNo unique trade number
     * @return cached order or null
     */
    UserOrderAggregate getCachedOrder(String outTradeNo);

    /**
     * Cache one order snapshot.
     *
     * @param order order aggregate
     * @param ttl   ttl
     */
    void cacheOrder(UserOrderAggregate order, Duration ttl);
}
