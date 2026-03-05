package com.busgallery.busgallery;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.busgallery.busgallery.mapper")
@SpringBootApplication
public class BusGalleryApplication {

    public static void main(String[] args) {
        SpringApplication.run(BusGalleryApplication.class, args);
    }
}