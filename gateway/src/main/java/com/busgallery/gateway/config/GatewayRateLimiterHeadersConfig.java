package com.busgallery.gateway.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.stereotype.Component;

/**
 * Disable redis rate-limiter response headers to avoid writing headers
 * after response becomes read-only in short-circuit scenarios.
 */
@Component
@RequiredArgsConstructor
public class GatewayRateLimiterHeadersConfig {

    private final RedisRateLimiter redisRateLimiter;

    @PostConstruct
    public void disableRateLimitHeaders() {
        redisRateLimiter.setIncludeHeaders(false);
    }
}
