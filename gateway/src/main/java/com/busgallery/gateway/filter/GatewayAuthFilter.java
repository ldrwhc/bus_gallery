package com.busgallery.gateway.filter;

import com.busgallery.gateway.config.GatewayAuthProperties;
import com.busgallery.gateway.model.SessionPayload;
import com.busgallery.gateway.util.HmacSigner;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Global authentication filter for gateway.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GatewayAuthFilter implements GlobalFilter, Ordered {

    private static final String HDR_REQUEST_ID = "X-Request-Id";
    private static final String HDR_INTERNAL_AUTH = "X-Internal-Auth";
    private static final String HDR_USER_ID = "X-User-Id";
    private static final String HDR_USERNAME = "X-Username";
    private static final String HDR_DISPLAY_NAME = "X-Display-Name";
    private static final String HDR_ROLE = "X-Role";
    private static final String HDR_REVIEW_REGION = "X-Review-Region";
    private static final String HDR_AUTH_TS = "X-Auth-Ts";
    private static final String HDR_AUTH_SIGNATURE = "X-Auth-Signature";

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final GatewayAuthProperties authProperties;

    @Override
    public int getOrder() {
        return -100;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!authProperties.isEnabled()) {
            return chain.filter(exchange);
        }
        String path = exchange.getRequest().getURI().getPath();
        String requestId = resolveRequestId(exchange.getRequest().getHeaders());
        ServerWebExchange requestIdExchange = mutateHeader(exchange, HDR_REQUEST_ID, requestId);

        String token = resolveToken(requestIdExchange.getRequest().getHeaders(), requestIdExchange.getRequest().getQueryParams().getFirst("token"));
        boolean authRequired = matchesPrefix(path, authProperties.getAuthRequiredPaths());
        boolean anonymousPath = matchesPrefix(path, authProperties.getAnonymousPaths());

        if (!StringUtils.hasText(token)) {
            if (authRequired) {
                return unauthorized(requestIdExchange, "A0401", "login required");
            }
            return chain.filter(requestIdExchange);
        }

        String key = authProperties.getSessionKeyPrefix() + token;
        return redisTemplate.opsForValue().get(key)
                .flatMap(raw -> applyInternalHeadersAndContinue(requestIdExchange, chain, raw, token))
                .switchIfEmpty(handleMissingSession(requestIdExchange, chain, authRequired, anonymousPath));
    }

    private Mono<Void> applyInternalHeadersAndContinue(ServerWebExchange exchange,
                                                       GatewayFilterChain chain,
                                                       String rawSession,
                                                       String token) {
        try {
            SessionPayload payload = objectMapper.readValue(rawSession, SessionPayload.class);
            if (payload == null || payload.getUserId() == null) {
                return unauthorized(exchange, "A0401", "invalid session payload");
            }
            long ts = Instant.now().getEpochSecond();
            String userId = String.valueOf(payload.getUserId());
            String username = safe(payload.getUsername());
            String displayName = safe(payload.getDisplayName());
            String role = StringUtils.hasText(payload.getRole()) ? payload.getRole().trim().toUpperCase() : "USER";
            String reviewRegion = payload.getReviewRegionId() == null ? "" : String.valueOf(payload.getReviewRegionId());
            String signBase = buildSignBase(userId, role, reviewRegion, ts);
            String signature = HmacSigner.signHex(authProperties.getInternalSecret(), signBase);

            ServerWebExchange next = exchange.mutate().request(exchange.getRequest().mutate()
                    .header(authProperties.getInternalAuthHeader(), "1")
                    .header(HDR_INTERNAL_AUTH, "1")
                    .header(HDR_USER_ID, userId)
                    .header(HDR_USERNAME, username)
                    .header(HDR_DISPLAY_NAME, displayName)
                    .header(HDR_ROLE, role)
                    .header(HDR_REVIEW_REGION, reviewRegion)
                    .header(HDR_AUTH_TS, String.valueOf(ts))
                    .header(HDR_AUTH_SIGNATURE, signature)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .build()).build();
            return chain.filter(next);
        } catch (Exception ex) {
            log.warn("parse session failed, reason={}", ex.getMessage());
            return unauthorized(exchange, "A0401", "invalid session payload");
        }
    }

    private Mono<Void> handleMissingSession(ServerWebExchange exchange,
                                            GatewayFilterChain chain,
                                            boolean authRequired,
                                            boolean anonymousPath) {
        if (authRequired) {
            return unauthorized(exchange, "A0401", "session expired");
        }
        if (anonymousPath) {
            return chain.filter(exchange);
        }
        // Keep public endpoints available when client still carries expired token.
        return chain.filter(exchange);
    }

    private ServerWebExchange mutateHeader(ServerWebExchange exchange, String name, String value) {
        if (!StringUtils.hasText(value)) {
            return exchange;
        }
        return exchange.mutate().request(exchange.getRequest().mutate().header(name, value).build()).build();
    }

    private String resolveRequestId(HttpHeaders headers) {
        String existing = headers.getFirst(HDR_REQUEST_ID);
        if (StringUtils.hasText(existing)) {
            return existing.trim();
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String resolveToken(HttpHeaders headers, String tokenParam) {
        String header = headers.getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header)) {
            String trimmed = header.trim();
            if (trimmed.toLowerCase().startsWith("bearer ")) {
                return trimmed.substring(7).trim();
            }
            return trimmed;
        }
        return StringUtils.hasText(tokenParam) ? tokenParam.trim() : null;
    }

    private boolean matchesPrefix(String path, List<String> prefixes) {
        if (!StringUtils.hasText(path) || prefixes == null || prefixes.isEmpty()) {
            return false;
        }
        for (String prefix : prefixes) {
            if (!StringUtils.hasText(prefix)) {
                continue;
            }
            if (path.startsWith(prefix.trim())) {
                return true;
            }
        }
        return false;
    }

    private String safe(String value) {
        return Objects.toString(value, "").trim();
    }

    private String buildSignBase(String userId, String role, String reviewRegion, long ts) {
        // Only sign stable ASCII fields to avoid non-ASCII header transport mismatch.
        return String.join("|",
                safe(userId),
                safe(role).toUpperCase(),
                safe(reviewRegion),
                String.valueOf(ts));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String code, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = String.format("{\"code\":\"%s\",\"message\":\"%s\",\"data\":null}", code, escape(message));
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
