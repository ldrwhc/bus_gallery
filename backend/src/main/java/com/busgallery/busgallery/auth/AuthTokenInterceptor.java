package com.busgallery.busgallery.auth;

import com.busgallery.busgallery.exception.BizException;
import com.busgallery.busgallery.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Resolve authenticated principal for each request.
 * Prefer gateway-injected internal auth headers and only fallback to token session
 * when auth.gateway.trust-only is disabled.
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

    private final UserSessionService userSessionService;

    @Value("${auth.gateway.trust-only:true}")
    private boolean trustGatewayOnly;

    @Value("${auth.gateway.internal-secret:change-me-gateway-secret}")
    private String gatewayInternalSecret;

    @Value("${auth.gateway.max-skew-seconds:120}")
    private long gatewayAuthMaxSkewSeconds;

    @Value("${auth.gateway.internal-auth-header:X-Internal-Auth}")
    private String gatewayInternalHeader;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            AuthPrincipal gatewayPrincipal = resolveGatewayPrincipal(request);
            if (gatewayPrincipal != null) {
                AuthContextHolder.set(gatewayPrincipal);
            } else if (!trustGatewayOnly) {
                String token = resolveToken(request);
                if (StringUtils.hasText(token)) {
                    UserSession session = userSessionService.getSession(token);
                    if (session != null) {
                        AuthContextHolder.set(AuthPrincipal.fromSession(session));
                    }
                }
            }
            if (handler instanceof HandlerMethod handlerMethod) {
                RequireLogin requireLogin = findRequireLogin(handlerMethod);
                if (requireLogin != null && AuthContextHolder.get() == null) {
                    throw new BizException(ErrorCode.UNAUTHORIZED, "请先登录");
                }
            }
            return true;
        } catch (BizException ex) {
            AuthContextHolder.clear();
            throw ex;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContextHolder.clear();
    }

    private RequireLogin findRequireLogin(HandlerMethod handlerMethod) {
        RequireLogin methodAnno = handlerMethod.getMethodAnnotation(RequireLogin.class);
        if (methodAnno != null) {
            return methodAnno;
        }
        return handlerMethod.getBeanType().getAnnotation(RequireLogin.class);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header)) {
            if (header.startsWith("Bearer ")) {
                return header.substring(7).trim();
            }
            return header.trim();
        }
        String tokenParam = request.getParameter("token");
        return StringUtils.hasText(tokenParam) ? tokenParam.trim() : null;
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
            throw new BizException(ErrorCode.UNAUTHORIZED, "Invalid gateway auth marker");
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
            throw new BizException(ErrorCode.UNAUTHORIZED, "Missing gateway auth headers");
        }

        long userId = parseLongOrThrow(userIdText, "Invalid gateway user id");
        long ts = parseLongOrThrow(tsText, "Invalid gateway auth timestamp");
        long now = Instant.now().getEpochSecond();
        if (Math.abs(now - ts) > Math.max(30L, gatewayAuthMaxSkewSeconds)) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "Gateway auth header expired");
        }

        String normalizedRole = roleText.trim().toUpperCase();
        String signatureText = signature.trim();
        if (!verifySignature(userId, username, displayName, normalizedRole, reviewRegionText, ts, signatureText)) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "Gateway auth signature mismatch");
        }

        Long reviewRegionId = null;
        if (StringUtils.hasText(reviewRegionText)) {
            reviewRegionId = parseLongOrThrow(reviewRegionText, "Invalid review region id");
        }

        return AuthPrincipal.builder()
                .userId(userId)
                .username(username)
                .displayName(displayName)
                .role(UserRole.fromValue(normalizedRole))
                .reviewRegionId(reviewRegionId)
                .token(resolveToken(request))
                .build();
    }

    private long parseLongOrThrow(String value, String message) {
        try {
            return Long.parseLong(value.trim());
        } catch (Exception ex) {
            throw new BizException(ErrorCode.UNAUTHORIZED, message);
        }
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String signHex(String secret, String payload) {
        if (!StringUtils.hasText(secret)) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "Gateway secret is not configured");
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
            throw new BizException(ErrorCode.UNAUTHORIZED, "Gateway signature verify failed");
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
}
