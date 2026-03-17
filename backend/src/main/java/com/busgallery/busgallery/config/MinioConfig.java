package com.busgallery.busgallery.config;

import com.busgallery.busgallery.service.storage.StorageProperties;
import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * MinioConfig类用于封装MinioConfig相关的领域职责（所在包：com.busgallery.busgallery.config）。
 */
@Configuration
@Profile("!test")
public class MinioConfig {

    /**
     * minioClient方法用于处理minioClient相关的业务逻辑。
     * @param properties properties参数，详见调用方上下文。
     * @return 返回MinioClient类型结果。
     */
    @Bean
    public MinioClient minioClient(StorageProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }
}