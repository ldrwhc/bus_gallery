package com.busgallery.busgallery.auth;

import com.busgallery.busgallery.exception.BizException;
import com.busgallery.busgallery.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuthTokenInterceptor implements HandlerInterceptor {

    private final UserSessionService userSessionService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            String token = resolveToken(request);
            if (StringUtils.hasText(token)) {
                UserSession session = userSessionService.getSession(token);
                if (session != null) {
                    AuthContextHolder.set(session);
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
}
