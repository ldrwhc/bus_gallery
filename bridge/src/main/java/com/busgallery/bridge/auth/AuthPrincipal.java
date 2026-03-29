package com.busgallery.bridge.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Auth principal resolved from gateway internal headers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthPrincipal {
    private Long userId;
    private String username;
    private String displayName;
    private String role;
    private Long reviewRegionId;
}
