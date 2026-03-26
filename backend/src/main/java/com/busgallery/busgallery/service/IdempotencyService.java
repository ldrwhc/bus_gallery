package com.busgallery.busgallery.service;

import com.busgallery.busgallery.exception.BizException;
import com.busgallery.busgallery.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Simple idempotency guard backed by Redis.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private static final String KEY_PREFIX = "idempotent:upload:";

    private final StringRedisTemplate redisTemplate;
    private final Map<String, Long> localLocks = new ConcurrentHashMap<>();

    public <T> T runOnce(String key, Duration ttl, Supplier<T> supplier) {
        if (!StringUtils.hasText(key)) {
            return supplier.get();
        }
        String redisKey = KEY_PREFIX + key.trim();
        Boolean redisLocked = tryAcquireRedisLock(redisKey, ttl);
        boolean useLocalLock = redisLocked == null;
        boolean locked = useLocalLock ? tryAcquireLocalLock(redisKey, ttl) : Boolean.TRUE.equals(redisLocked);
        if (!locked) {
            throw new BizException(ErrorCode.REQUEST_DUPLICATE, "Duplicate request detected");
        }
        try {
            return supplier.get();
        } catch (RuntimeException e) {
            if (useLocalLock) {
                localLocks.remove(redisKey);
            } else {
                safeReleaseRedisLock(redisKey);
            }
            throw e;
        }
    }

    private Boolean tryAcquireRedisLock(String redisKey, Duration ttl) {
        try {
            return redisTemplate.opsForValue().setIfAbsent(redisKey, "1", ttl);
        } catch (Exception ex) {
            log.warn("Redis idempotency lock unavailable, fallback to local lock. key={}, reason={}",
                    redisKey, rootMessage(ex));
            return null;
        }
    }

    private void safeReleaseRedisLock(String redisKey) {
        try {
            redisTemplate.delete(redisKey);
        } catch (Exception ex) {
            log.warn("Redis idempotency lock release skipped. key={}, reason={}",
                    redisKey, rootMessage(ex));
        }
    }

    private boolean tryAcquireLocalLock(String key, Duration ttl) {
        long now = System.currentTimeMillis();
        long ttlMs = Math.max(1000L, ttl == null ? 60_000L : ttl.toMillis());
        long expiresAt = now + ttlMs;
        cleanupExpiredLocalLocks(now);
        Long previous = localLocks.putIfAbsent(key, expiresAt);
        if (previous == null) {
            return true;
        }
        if (previous <= now && localLocks.replace(key, previous, expiresAt)) {
            return true;
        }
        return false;
    }

    private void cleanupExpiredLocalLocks(long now) {
        localLocks.entrySet().removeIf(entry -> entry.getValue() != null && entry.getValue() <= now);
    }

    private String rootMessage(Throwable e) {
        Throwable cursor = e;
        Throwable last = e;
        while (cursor != null) {
            last = cursor;
            cursor = cursor.getCause();
        }
        return last == null ? "" : String.valueOf(last.getMessage());
    }
}
