package com.busgallery.bridge.auth;

/**
 * Thread local auth context for current request.
 */
public final class AuthContextHolder {

    private static final ThreadLocal<AuthPrincipal> CONTEXT = new ThreadLocal<>();

    private AuthContextHolder() {
    }

    public static void set(AuthPrincipal principal) {
        CONTEXT.set(principal);
    }

    public static AuthPrincipal getPrincipal() {
        return CONTEXT.get();
    }

    public static AuthPrincipal requirePrincipal() {
        AuthPrincipal principal = CONTEXT.get();
        if (principal == null || principal.getUserId() == null) {
            throw new IllegalStateException("login required");
        }
        return principal;
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
