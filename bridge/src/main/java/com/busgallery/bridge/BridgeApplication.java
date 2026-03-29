package com.busgallery.bridge;

import com.busgallery.bridge.config.BridgeProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Bridge application entry point.
 */
@SpringBootApplication
@EnableFeignClients(basePackages = "com.busgallery.bridge.client")
@EnableConfigurationProperties(BridgeProperties.class)
public class BridgeApplication {

    /**
     * Main method.
     *
     * @param args start args
     */
    public static void main(String[] args) {
        SpringApplication.run(BridgeApplication.class, args);
    }
}
