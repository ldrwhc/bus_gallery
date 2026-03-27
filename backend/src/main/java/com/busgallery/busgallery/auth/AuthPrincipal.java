package com.busgallery.busgallery.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight authenticated principal stored in ThreadLocal per request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthPrincipal {
    private Long userId;
    private String username;
    private String displayName;
    private String avatarUrl;
    private UserRole role;
    private Long reviewRegionId;
    private String token;

    public static AuthPrincipal fromSession(UserSession session) {
        if (session == null) {
            return null;
        }
        return AuthPrincipal.builder()
                .userId(session.getUserId())
                .username(session.getUsername())
                .displayName(session.getDisplayName())
                .avatarUrl(session.getAvatarUrl())
                .role(session.getRole())
                .reviewRegionId(session.getReviewRegionId())
                .token(session.getToken())
                .build();
    }

    public boolean isStation() {
        return role == UserRole.STATION;
    }

    public boolean isReviewer() {
        return role == UserRole.REVIEWER;
    }

    public boolean isReviewerOrStation() {
        return isStation() || isReviewer();
    }
}

