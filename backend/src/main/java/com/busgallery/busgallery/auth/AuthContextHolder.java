package com.busgallery.busgallery.auth;

import com.busgallery.busgallery.exception.BizException;
import com.busgallery.busgallery.exception.ErrorCode;
import lombok.experimental.UtilityClass;

/**
 * AuthContextHolder类用于封装AuthContextHolder相关的领域职责（所在包：com.busgallery.busgallery.auth）。
 */
@UtilityClass
public class AuthContextHolder {

    private static final ThreadLocal<UserSession> CONTEXT = new ThreadLocal<>();

    /**
     * set方法用于处理set相关的业务逻辑。
     * @param session session参数，详见调用方上下文。
     * @return 无返回值。
     */
    public static void set(UserSession session) {
        CONTEXT.set(session);
    }

    /**
     * get方法用于处理get相关的业务逻辑。
     * @return 返回UserSession类型结果。
     */
    public static UserSession get() {
        return CONTEXT.get();
    }

    /**
     * requireUser方法用于处理requireUser相关的业务逻辑。
     * @return 返回UserSession类型结果。
     */
    public static UserSession requireUser() {
        UserSession session = get();
        if (session == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        return session;
    }

    /**
     * clear方法用于处理clear相关的业务逻辑。
     * @return 无返回值。
     */
    public static void clear() {
        CONTEXT.remove();
    }
}
