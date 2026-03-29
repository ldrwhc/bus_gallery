package com.busgallery.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Gateway authentication properties.
 */
@Data
@Component
@ConfigurationProperties(prefix = "gateway.auth")
public class GatewayAuthProperties {

    /**
     * Enable gateway auth logic.
     */
    private boolean enabled = true;

    /**
     * Redis key prefix for token sessions.
     */
    private String sessionKeyPrefix = "busgallery:sessions:";

    /**
     * Secret for internal header signing.
     */
    private String internalSecret = "change-me-gateway-secret";

    /**
     * Internal marker header name.
     */
    private String internalAuthHeader = "X-Internal-Auth";

    /**
     * Path prefixes that require authenticated session at gateway layer.
     */
    private List<String> authRequiredPaths = new ArrayList<>(List.of("/api/bridge/"));

    /**
     * Path prefixes that can be anonymous.
     */
    private List<String> anonymousPaths = new ArrayList<>(List.of("/api/auth/"));
}
