package com.busgallery.groupbuy.trigger.auth;

import com.busgallery.groupbuy.types.enums.GroupErrorCode;
import com.busgallery.groupbuy.types.exception.GroupBizException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Resolve authenticated principal for trigger layer.
 * Gateway internal auth headers are preferred and validated by HMAC signature.
 */
@Component
@RequiredArgsConstructor
public class AuthTokenInterceptor implements HandlerInterceptor {

    private static final String HMAC_ALGO = "HmacSHA256";
    private static final String HDR_INTERNAL_AUTH = "X-Internal-Auth";
    private static final String HDR_USER_ID = "X-User-Id";
    private static final String HDR_USERNAME = "X-Username";
    private static final String HDR_DISPLAY_NAME = "X-Display-Name";
    private static final String HDR_ROLE = "X-Role";
    private static final String HDR_REVIEW_REGION = "X-Review-Region";
    private static final String HDR_AUTH_TS = "X-Auth-Ts";
    private static final String HDR_AUTH_SIGNATURE = "X-Auth-Signature";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${group.auth.session-key-prefix:busgallery:sessions:}")
    private String sessionKeyPrefix;

    @Value("${group.auth.gateway.trust-only:true}")
    private boolean trustGatewayOnly;

    @Value("${group.auth.gateway.internal-secret:change-me-gateway-secret}")
    private String gatewayInternalSecret;

    @Value("${group.auth.gateway.max-skew-seconds:120}")
    private long gatewayAuthMaxSkewSeconds;

    @Value("${group.auth.gateway.internal-auth-header:X-Internal-Auth}")
    private String gatewayInternalHeader;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        try {
            AuthPrincipal principal = resolveGatewayPrincipal(request);
            if (principal == null && !trustGatewayOnly) {
                String token = resolveToken(request);
                if (StringUtils.hasText(token)) {
                    principal = loadPrincipalByToken(token);
                }
            }
            if (principal != null) {
                AuthContextHolder.set(principal);
            }

            if (handler instanceof HandlerMethod method) {
                RequireLogin requireLogin = findRequireLogin(method);
                if (requireLogin != null && AuthContextHolder.getPrincipal() == null) {
                    throw new GroupBizException(GroupErrorCode.UNAUTHORIZED, "login required");
                }
            }
            return true;
        } catch (GroupBizException ex) {
            AuthContextHolder.clear();
            throw ex;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContextHolder.clear();
    }

    private AuthPrincipal resolveGatewayPrincipal(HttpServletRequest request) {
        String marker = request.getHeader(StringUtils.hasText(gatewayInternalHeader) ? gatewayInternalHeader : HDR_INTERNAL_AUTH);
        if (!StringUtils.hasText(marker)) {
            marker = request.getHeader(HDR_INTERNAL_AUTH);
        }
        if (!StringUtils.hasText(marker)) {
            return null;
        }
        if (!"1".equals(marker.trim())) {
            throw new GroupBizException(GroupErrorCode.UNAUTHORIZED, "invalid gateway auth marker");
        }

        String userIdText = request.getHeader(HDR_USER_ID);
        String roleText = request.getHeader(HDR_ROLE);
        String username = trimToEmpty(request.getHeader(HDR_USERNAME));
        String displayName = trimToEmpty(request.getHeader(HDR_DISPLAY_NAME));
        String reviewRegionText = trimToEmpty(request.getHeader(HDR_REVIEW_REGION));
        String tsText = request.getHeader(HDR_AUTH_TS);
        String signature = request.getHeader(HDR_AUTH_SIGNATURE);

        if (!StringUtils.hasText(userIdText)
                || !StringUtils.hasText(roleText)
                || !StringUtils.hasText(tsText)
                || !StringUtils.hasText(signature)) {
            throw new GroupBizException(GroupErrorCode.UNAUTHORIZED, "missing gateway auth headers");
        }

        long userId = parseLongOrThrow(userIdText, "invalid gateway user id");
        long ts = parseLongOrThrow(tsText, "invalid gateway auth timestamp");
        long now = Instant.now().getEpochSecond();
        if (Math.abs(now - ts) > Math.max(30L, gatewayAuthMaxSkewSeconds)) {
            throw new GroupBizException(GroupErrorCode.UNAUTHORIZED, "gateway auth header expired");
        }

        String normalizedRole = roleText.trim().toUpperCase();
        String signatureText = signature.trim();
        if (!verifySignature(userId, username, displayName, normalizedRole, reviewRegionText, ts, signatureText)) {
            throw new GroupBizException(GroupErrorCode.UNAUTHORIZED, "gateway auth signature mismatch");
        }

        Long reviewRegionId = null;
        if (StringUtils.hasText(reviewRegionText)) {
            reviewRegionId = parseLongOrThrow(reviewRegionText, "invalid review region id");
        }

        return AuthPrincipal.builder()
                .userId(userId)
                .username(username)
                .displayName(displayName)
                .role(normalizedRole)
                .reviewRegionId(reviewRegionId)
                .token(resolveToken(request))
                .build();
    }

    private AuthPrincipal loadPrincipalByToken(String token) {
        String raw = stringRedisTemplate.opsForValue().get(sessionKeyPrefix + token);
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            SessionPayload payload = objectMapper.readValue(raw, SessionPayload.class);
            if (payload.getUserId() == null) {
                return null;
            }
            return AuthPrincipal.builder()
                    .userId(payload.getUserId())
                    .username(payload.getUsername())
                    .displayName(payload.getDisplayName())
                    .avatarUrl(payload.getAvatarUrl())
                    .role(payload.getRole())
                    .reviewRegionId(payload.getReviewRegionId())
                    .token(token)
                    .build();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header)) {
            if (header.startsWith("Bearer ")) {
                return header.substring(7).trim();
            }
            return header.trim();
        }
        String token = request.getParameter("token");
        if (StringUtils.hasText(token)) {
            return token.trim();
        }
        return null;
    }

    private RequireLogin findRequireLogin(HandlerMethod method) {
        RequireLogin methodAnno = method.getMethodAnnotation(RequireLogin.class);
        if (methodAnno != null) {
            return methodAnno;
        }
        return method.getBeanType().getAnnotation(RequireLogin.class);
    }

    private long parseLongOrThrow(String value, String message) {
        try {
            return Long.parseLong(value.trim());
        } catch (Exception ex) {
            throw new GroupBizException(GroupErrorCode.UNAUTHORIZED, message);
        }
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String signHex(String secret, String payload) {
        if (!StringUtils.hasText(secret)) {
            throw new GroupBizException(GroupErrorCode.UNAUTHORIZED, "gateway secret is not configured");
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new GroupBizException(GroupErrorCode.UNAUTHORIZED, "gateway signature verify failed");
        }
    }

    private boolean verifySignature(long userId,
                                    String username,
                                    String displayName,
                                    String normalizedRole,
                                    String reviewRegionText,
                                    long ts,
                                    String signature) {
        String v2Base = String.join("|",
                String.valueOf(userId),
                normalizedRole,
                reviewRegionText,
                String.valueOf(ts));
        String v2Expected = signHex(gatewayInternalSecret, v2Base);
        if (v2Expected.equalsIgnoreCase(signature)) {
            return true;
        }
        String legacyBase = String.join("|",
                String.valueOf(userId),
                username,
                displayName,
                normalizedRole,
                reviewRegionText,
                String.valueOf(ts));
        String legacyExpected = signHex(gatewayInternalSecret, legacyBase);
        return legacyExpected.equalsIgnoreCase(signature);
    }

    /**
     * Compatible shape with main platform Redis session payload.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SessionPayload {
        private Long userId;
        private String username;
        private String displayName;
        private String avatarUrl;
        private String role;
        private Long reviewRegionId;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getAvatarUrl() {
            return avatarUrl;
        }

        public void setAvatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public Long getReviewRegionId() {
            return reviewRegionId;
        }

        public void setReviewRegionId(Long reviewRegionId) {
            this.reviewRegionId = reviewRegionId;
        }
    }
}
