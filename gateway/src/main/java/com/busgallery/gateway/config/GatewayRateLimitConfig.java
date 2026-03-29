package com.busgallery.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

/**
 * Gateway rate-limit key resolver configuration.
 */
@Configuration
public class GatewayRateLimitConfig {

    /**
     * Prefer authenticated user id as rate-limit key, fallback to request IP.
     */
    @Bean(name = "userOrIpKeyResolver")
    public KeyResolver userOrIpKeyResolver() {
        return exchange -> {
            HttpHeaders headers = exchange.getRequest().getHeaders();
            String userId = trim(headers.getFirst("X-User-Id"));
            if (StringUtils.hasText(userId)) {
                return Mono.just("u:" + userId);
            }
            String realIp = trim(headers.getFirst("X-Real-IP"));
            if (StringUtils.hasText(realIp)) {
                return Mono.just("ip:" + realIp);
            }
            String forwardedFor = trim(headers.getFirst("X-Forwarded-For"));
            if (StringUtils.hasText(forwardedFor)) {
                int commaIndex = forwardedFor.indexOf(',');
                String firstHop = commaIndex > -1 ? forwardedFor.substring(0, commaIndex).trim() : forwardedFor;
                if (StringUtils.hasText(firstHop)) {
                    return Mono.just("ip:" + firstHop);
                }
            }
            if (exchange.getRequest().getRemoteAddress() != null
                    && exchange.getRequest().getRemoteAddress().getAddress() != null) {
                return Mono.just("ip:" + exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
            }
            return Mono.just("ip:unknown");
        };
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
