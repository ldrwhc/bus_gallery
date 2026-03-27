package com.busgallery.busgallery.auth;

import com.busgallery.busgallery.exception.BizException;
import com.busgallery.busgallery.exception.ErrorCode;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AuthContextHolder {

    private static final ThreadLocal<AuthPrincipal> CONTEXT = new ThreadLocal<>();

    public static void set(AuthPrincipal principal) {
        CONTEXT.set(principal);
    }

    public static AuthPrincipal get() {
        return CONTEXT.get();
    }

    public static AuthPrincipal getPrincipal() {
        return CONTEXT.get();
    }

    public static AuthPrincipal requireUser() {
        AuthPrincipal principal = get();
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        return principal;
    }

    public static AuthPrincipal requirePrincipal() {
        return requireUser();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}

