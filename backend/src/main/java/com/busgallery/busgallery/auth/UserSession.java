package com.busgallery.busgallery.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
