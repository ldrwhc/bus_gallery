package com.busgallery.groupbuy.trigger.auth;

import com.busgallery.groupbuy.types.enums.GroupErrorCode;
import com.busgallery.groupbuy.types.exception.GroupBizException;
import lombok.experimental.UtilityClass;

/**
 * ThreadLocal holder for current authenticated principal.
 */
@UtilityClass
public class AuthContextHolder {
    private static final ThreadLocal<AuthPrincipal> CONTEXT = new ThreadLocal<>();

    /**
     * Bind current principal.
     *
     * @param principal principal
     */
    public static void set(AuthPrincipal principal) {
        CONTEXT.set(principal);
    }

    /**
     * Get current principal.
     *
     * @return principal or null
     */
    public static AuthPrincipal getPrincipal() {
        return CONTEXT.get();
    }

    /**
     * Get current principal and throw if missing.
     *
     * @return principal
     */
    public static AuthPrincipal requirePrincipal() {
        AuthPrincipal principal = CONTEXT.get();
        if (principal == null || principal.getUserId() == null) {
            throw new GroupBizException(GroupErrorCode.UNAUTHORIZED, "login required");
        }
        return principal;
    }

    /**
     * Clear context.
     */
    public static void clear() {
        CONTEXT.remove();
    }
}
