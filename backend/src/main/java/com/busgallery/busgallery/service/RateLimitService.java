package com.busgallery.busgallery.service;

import com.busgallery.busgallery.exception.BizException;
import com.busgallery.busgallery.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private static final String KEY_PREFIX = "busgallery:rl:";

    private final StringRedisTemplate redisTemplate;

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
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, window.plusSeconds(2));
        }
        if (count != null && count > limit) {
            throw new BizException(ErrorCode.REQUEST_DUPLICATE, message);
        }
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
}

