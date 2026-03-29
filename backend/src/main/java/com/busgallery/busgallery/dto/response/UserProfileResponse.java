package com.busgallery.busgallery.dto.response;

import com.busgallery.busgallery.auth.UserRole;
import lombok.Builder;
import lombok.Data;

/**
 * UserProfileResponse类用于封装UserProfileResponse相关的领域职责（所在包：com.busgallery.busgallery.dto.response）。
 */
@Data
@Builder
public class UserProfileResponse {
    private Long id;
    private String username;
    private String displayName;
    private String avatarUrl;
    private String bio;
    private String emailMasked;
    private boolean emailVerified;
    private long balanceCents;
    private UserRole role;
    private Long reviewRegionId;
    private long uploadsCount;
}
