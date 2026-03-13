package com.busgallery.busgallery.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "auth.session")
public class AuthProperties {

    /**
     * Session TTL in seconds for cached user info.
     */
    private long ttlSeconds = 86_400L;
}
