package com.busgallery.busgallery.service;

import com.busgallery.busgallery.exception.BizException;
import com.busgallery.busgallery.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private static final String KEY_PREFIX = "busgallery:rl:";
    private static final int LOCAL_COUNTER_MAX_SIZE = 50_000;
    private static final long REDIS_FALLBACK_WARN_INTERVAL_MS = 60_000L;

    private final StringRedisTemplate redisTemplate;
    private final Map<String, LocalCounter> localCounters = new ConcurrentHashMap<>();
    private final AtomicLong lastRedisFallbackWarnAt = new AtomicLong(0L);

    public void check(String scope,
                      String dimension,
                      String identity,
                      long limit,
                      Duration window,
                      String message) {
        if (!StringUtils.hasText(scope) || !StringUtils.hasText(dimension) || !StringUtils.hasText(identity)) {
            return;
        }
        if (limit <= 0 || window == null || window.isNegative() || window.isZero()) {
            return;
        }
        long seconds = Math.max(1L, window.getSeconds());
        long slot = Instant.now().getEpochSecond() / seconds;
        String key = KEY_PREFIX + scope + ":" + dimension + ":" + sha256(identity.trim().toLowerCase()) + ":" + slot;
        Long count = incrementCounter(key, window);
        if (count != null && count > limit) {
            throw new BizException(ErrorCode.REQUEST_DUPLICATE, message);
        }
    }

    private Long incrementCounter(String key, Duration window) {
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, window.plusSeconds(2));
            }
            return count;
        } catch (Exception ex) {
            logRedisFallback(ex);
            return incrementLocalCounter(key, window);
        }
    }

    private long incrementLocalCounter(String key, Duration window) {
        long now = System.currentTimeMillis();
        long expireAt = now + window.toMillis() + 2_000L;
        LocalCounter counter = localCounters.compute(key, (k, existing) -> {
            if (existing == null || existing.expireAt <= now) {
                return new LocalCounter(new AtomicLong(1L), expireAt);
            }
            existing.expireAt = Math.max(existing.expireAt, expireAt);
            existing.counter.incrementAndGet();
            return existing;
        });
        maybeCleanupLocalCounters(now);
        return counter == null ? 1L : counter.counter.get();
    }

    private void maybeCleanupLocalCounters(long now) {
        if (localCounters.size() <= LOCAL_COUNTER_MAX_SIZE && ThreadLocalRandom.current().nextInt(100) != 0) {
            return;
        }
        localCounters.entrySet().removeIf(entry -> entry.getValue().expireAt <= now);
        if (localCounters.size() <= LOCAL_COUNTER_MAX_SIZE) {
            return;
        }
        int removed = 0;
        for (String key : localCounters.keySet()) {
            localCounters.remove(key);
            removed++;
            if (localCounters.size() <= LOCAL_COUNTER_MAX_SIZE || removed >= 2_000) {
                break;
            }
        }
    }

    private void logRedisFallback(Exception ex) {
        long now = System.currentTimeMillis();
        long last = lastRedisFallbackWarnAt.get();
        if (now - last < REDIS_FALLBACK_WARN_INTERVAL_MS || !lastRedisFallbackWarnAt.compareAndSet(last, now)) {
            return;
        }
        log.warn("Redis unavailable for rate limit, switched to in-memory fallback: {}", ex.getMessage());
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception ex) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private static final class LocalCounter {
        private final AtomicLong counter;
        private volatile long expireAt;

        private LocalCounter(AtomicLong counter, long expireAt) {
            this.counter = counter;
            this.expireAt = expireAt;
        }
    }
}
