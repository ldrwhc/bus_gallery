package com.busgallery.groupbuy.trigger.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Authenticated principal bridged from bus-gallery session token.
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
    private String role;
    private Long reviewRegionId;
    private String token;
}
