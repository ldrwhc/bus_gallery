package com.busgallery.busgallery.dto.response;

import com.busgallery.busgallery.auth.UserRole;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminUserResponse {
    private Long id;
    private String username;
    private String displayName;
    private UserRole role;
    private Long reviewRegionId;
}
