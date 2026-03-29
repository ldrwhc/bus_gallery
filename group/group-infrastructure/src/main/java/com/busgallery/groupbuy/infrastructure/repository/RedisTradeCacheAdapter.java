package com.busgallery.groupbuy.infrastructure.repository;

import com.busgallery.groupbuy.domain.model.UserOrderAggregate;
import com.busgallery.groupbuy.domain.port.TradeCachePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * Redis adapter for idempotency lock and order snapshot cache.
 */
@Component
@RequiredArgsConstructor
public class RedisTradeCacheAdapter implements TradeCachePort {
    private static final String OUT_TRADE_LOCK_PREFIX = "group:lock:outTradeNo:";
    private static final String ORDER_CACHE_PREFIX = "group:order:outTradeNo:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public boolean tryLockOutTradeNo(String outTradeNo, Duration ttl) {
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(OUT_TRADE_LOCK_PREFIX + outTradeNo, "1", ttl);
        return Boolean.TRUE.equals(success);
    }

    @Override
    public void unlockOutTradeNo(String outTradeNo) {
        redisTemplate.delete(OUT_TRADE_LOCK_PREFIX + outTradeNo);
    }

    @Override
    public UserOrderAggregate getCachedOrder(String outTradeNo) {
        String raw = redisTemplate.opsForValue().get(ORDER_CACHE_PREFIX + outTradeNo);
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, UserOrderAggregate.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public void cacheOrder(UserOrderAggregate order, Duration ttl) {
        if (order == null || !StringUtils.hasText(order.getOutTradeNo())) {
            return;
        }
        try {
            String raw = objectMapper.writeValueAsString(order);
            redisTemplate.opsForValue().set(ORDER_CACHE_PREFIX + order.getOutTradeNo(), raw, ttl);
        } catch (Exception ignored) {
            // ignore cache write failures
        }
    }
}
