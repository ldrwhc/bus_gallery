package com.busgallery.gateway.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Session payload stored in Redis by content service.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionPayload {
    private Long userId;
    private String username;
    private String displayName;
    private String role;
    private Long reviewRegionId;
}
