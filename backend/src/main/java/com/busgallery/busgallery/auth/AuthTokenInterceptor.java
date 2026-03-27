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

/**
 * AuthTokenInterceptor类用于封装AuthTokenInterceptor相关的领域职责（所在包：com.busgallery.busgallery.auth）。
 */
@Component
@RequiredArgsConstructor
public class AuthTokenInterceptor implements HandlerInterceptor {

    private final UserSessionService userSessionService;

    /**
     * preHandle方法用于处理preHandle相关的业务逻辑。
     * @param request request参数，详见调用方上下文。
     * @param response response参数，详见调用方上下文。
     * @param handler handler参数，详见调用方上下文。
     * @return 返回boolean类型结果。
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            String token = resolveToken(request);
            if (StringUtils.hasText(token)) {
                UserSession session = userSessionService.getSession(token);
                if (session != null) {
                    AuthContextHolder.set(AuthPrincipal.fromSession(session));
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

    /**
     * afterCompletion方法用于处理afterCompletion相关的业务逻辑。
     * @param request request参数，详见调用方上下文。
     * @param response response参数，详见调用方上下文。
     * @param handler handler参数，详见调用方上下文。
     * @param ex ex参数，详见调用方上下文。
     * @return 无返回值。
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContextHolder.clear();
    }

    /**
     * findRequireLogin方法用于处理findRequireLogin相关的业务逻辑。
     * @param handlerMethod handlerMethod参数，详见调用方上下文。
     * @return 返回RequireLogin类型结果。
     */
    private RequireLogin findRequireLogin(HandlerMethod handlerMethod) {
        RequireLogin methodAnno = handlerMethod.getMethodAnnotation(RequireLogin.class);
        if (methodAnno != null) {
            return methodAnno;
        }
        return handlerMethod.getBeanType().getAnnotation(RequireLogin.class);
    }

    /**
     * resolveToken方法用于处理resolveToken相关的业务逻辑。
     * @param request request参数，详见调用方上下文。
     * @return 返回String类型结果。
     */
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
