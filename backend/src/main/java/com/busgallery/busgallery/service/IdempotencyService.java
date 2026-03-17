package com.busgallery.busgallery.service;

import com.busgallery.busgallery.exception.BizException;
import com.busgallery.busgallery.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Simple idempotency guard backed by Redis.
 */
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final String KEY_PREFIX = "idempotent:upload:";

    private final StringRedisTemplate redisTemplate;

    public <T> T runOnce(String key, Duration ttl, Supplier<T> supplier) {
        if (!StringUtils.hasText(key)) {
            return supplier.get();
        }
        String redisKey = KEY_PREFIX + key.trim();
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(redisKey, "1", ttl);
        if (Boolean.FALSE.equals(locked)) {
            throw new BizException(ErrorCode.REQUEST_DUPLICATE, "Duplicate request detected");
        }
        try {
            return supplier.get();
        } catch (RuntimeException e) {
            redisTemplate.delete(redisKey);
            throw e;
        }
    }
}
