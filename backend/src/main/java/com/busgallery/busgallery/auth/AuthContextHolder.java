package com.busgallery.busgallery.auth;

import com.busgallery.busgallery.exception.BizException;
import com.busgallery.busgallery.exception.ErrorCode;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AuthContextHolder {

    private static final ThreadLocal<UserSession> CONTEXT = new ThreadLocal<>();

    public static void set(UserSession session) {
        CONTEXT.set(session);
    }

    public static UserSession get() {
        return CONTEXT.get();
    }

    public static UserSession requireUser() {
        UserSession session = get();
        if (session == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        return session;
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
