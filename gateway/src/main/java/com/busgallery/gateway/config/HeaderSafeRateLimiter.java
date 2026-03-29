package com.busgallery.gateway.config;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;

/**
 * Wrapper rate limiter that strips response headers to avoid
 * RequestRateLimiter writing into read-only headers.
 */
@Component
@Primary
@RequiredArgsConstructor
public class HeaderSafeRateLimiter implements RateLimiter<RedisRateLimiter.Config> {

    private final RedisRateLimiter delegate;

    @Override
    public Mono<Response> isAllowed(String routeId, String id) {
        return delegate.isAllowed(routeId, id)
                .map(response -> new Response(response.isAllowed(), Collections.emptyMap()));
    }

    @Override
    public Map<String, RedisRateLimiter.Config> getConfig() {
        return delegate.getConfig();
    }

    @Override
    public Class<RedisRateLimiter.Config> getConfigClass() {
        return delegate.getConfigClass();
    }

    @Override
    public RedisRateLimiter.Config newConfig() {
        return delegate.newConfig();
    }
}
