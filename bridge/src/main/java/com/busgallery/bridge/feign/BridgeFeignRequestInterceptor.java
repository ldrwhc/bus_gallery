package com.busgallery.bridge.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

/**
 * Forward selected headers from inbound bridge request to downstream calls.
 */
@Component
public class BridgeFeignRequestInterceptor implements RequestInterceptor {

    private static final List<String> FORWARDED_HEADERS = List.of(
            "Authorization",
            "X-Request-Id",
            "X-Correlation-Id",
            "X-Internal-Auth",
            "X-User-Id",
            "X-Username",
            "X-Display-Name",
            "X-Role",
            "X-Review-Region",
            "X-Auth-Ts",
            "X-Auth-Signature"
    );

    @Override
    public void apply(RequestTemplate template) {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof ServletRequestAttributes servletAttrs)) {
            return;
        }
        HttpServletRequest request = servletAttrs.getRequest();
        if (request == null) {
            return;
        }
        for (String headerName : FORWARDED_HEADERS) {
            String headerValue = request.getHeader(headerName);
            if (StringUtils.hasText(headerValue)) {
                template.header(headerName, headerValue.trim());
            }
        }
    }
}
