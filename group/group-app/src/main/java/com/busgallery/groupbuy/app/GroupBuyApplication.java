package com.busgallery.groupbuy.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the group-buy service.
 */
@SpringBootApplication(scanBasePackages = "com.busgallery.groupbuy")
@EnableScheduling
public class GroupBuyApplication {

    /**
     * Main function.
     *
     * @param args startup arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(GroupBuyApplication.class, args);
    }
}
