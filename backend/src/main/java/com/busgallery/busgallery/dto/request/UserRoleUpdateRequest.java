package com.busgallery.busgallery.dto.request;

import com.busgallery.busgallery.auth.UserRole;
import lombok.Data;

@Data
public class UserRoleUpdateRequest {
    private UserRole role;
    private Long reviewRegionId;
}
