package com.busgallery.busgallery.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AuthProperties类用于封装AuthProperties相关的领域职责（所在包：com.busgallery.busgallery.config）。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "auth.session")
public class AuthProperties {

    /**
     * Session TTL in seconds for cached user info.
     */
    private long ttlSeconds = 86_400L;
}
