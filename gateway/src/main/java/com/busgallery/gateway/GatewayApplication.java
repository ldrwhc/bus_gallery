package com.busgallery.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Gateway application entry.
 */
@SpringBootApplication
public class GatewayApplication {

    /**
     * Main entry.
     *
     * @param args startup args
     */
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
