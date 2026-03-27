package com.busgallery.busgallery.auth;

import com.busgallery.busgallery.exception.BizException;
import com.busgallery.busgallery.exception.ErrorCode;

public final class RoleGuard {

    private RoleGuard() {
    }

    public static AuthPrincipal requireSession() {
        return AuthContextHolder.requirePrincipal();
    }

    public static AuthPrincipal requireStation() {
        AuthPrincipal session = requireSession();
        if (!session.isStation()) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "Only station role can access this resource");
        }
        return session;
    }

    public static AuthPrincipal requireReviewerOrStation() {
        AuthPrincipal session = requireSession();
        if (!session.isReviewerOrStation()) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "Review permission required");
        }
        return session;
    }
}
