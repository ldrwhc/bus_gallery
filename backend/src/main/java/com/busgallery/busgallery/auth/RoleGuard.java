package com.busgallery.busgallery.auth;

import com.busgallery.busgallery.exception.BizException;
import com.busgallery.busgallery.exception.ErrorCode;

public final class RoleGuard {

    private RoleGuard() {
    }

    public static UserSession requireSession() {
        return AuthContextHolder.requireUser();
    }

    public static UserSession requireStation() {
        UserSession session = requireSession();
        if (session.getRole() != UserRole.STATION) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "Only station role can access this resource");
        }
        return session;
    }

    public static UserSession requireReviewerOrStation() {
        UserSession session = requireSession();
        if (session.getRole() != UserRole.STATION && session.getRole() != UserRole.REVIEWER) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "Review permission required");
        }
        return session;
    }
}
