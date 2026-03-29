package com.busgallery.bridge.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP client bean configuration for bridge service.
 */
@Configuration
public class BridgeHttpClientConfig {

    /**
     * Build load-balanced RestTemplate for service-name based calls.
     *
     * @param builder base builder
     * @return load-balanced rest template
     */
    @Bean
    @LoadBalanced
    public RestTemplate loadBalancedRestTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}

