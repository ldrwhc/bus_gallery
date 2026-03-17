package com.busgallery.busgallery;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * BusGalleryApplication类用于封装BusGalleryApplication相关的领域职责（所在包：com.busgallery.busgallery）。
 */
@MapperScan("com.busgallery.busgallery.mapper")
@SpringBootApplication
public class BusGalleryApplication {

    /**
     * main方法用于处理main相关的业务逻辑。
     * @param args args参数，详见调用方上下文。
     * @return 无返回值。
     */
    public static void main(String[] args) {
        SpringApplication.run(BusGalleryApplication.class, args);
    }
}