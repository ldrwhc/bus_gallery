package com.busgallery.busgallery.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UserSession类用于封装UserSession相关的领域职责（所在包：com.busgallery.busgallery.auth）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {
    private Long userId;
    private String username;
    private String displayName;
    private String avatarUrl;
    private String token;
}
