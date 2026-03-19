package com.busgallery.busgallery.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public final class RequestIpUtil {

    private RequestIpUtil() {
    }

    public static String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        String xReal = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xReal)) {
            return xReal.trim();
        }
        String remote = request.getRemoteAddr();
        return StringUtils.hasText(remote) ? remote.trim() : "unknown";
    }
}

