package com.busgallery.busgallery.service.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "minio")
public class StorageProperties {

    /**
     * MinIO 服务地址，例如 http://minio:9000
     */
    private String endpoint;

    /**
     * 存储桶名称
     */
    private String bucket;

    private String accessKey;

    private String secretKey;

    /**
     * 可选的 CDN 或外网访问域名（若为空则使用 endpoint/bucket）
     */
    private String cdnHost;
}