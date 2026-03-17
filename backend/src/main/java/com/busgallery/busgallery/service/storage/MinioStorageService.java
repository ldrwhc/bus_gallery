package com.busgallery.busgallery.service.storage;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.InputStream;

/**
 * MinioStorageService类用于封装MinioStorageService相关的领域职责（所在包：com.busgallery.busgallery.service.storage）。
 */
@Service
@Profile("!test")
@RequiredArgsConstructor
public class MinioStorageService implements StorageService {

    private final MinioClient minioClient;
    private final StorageProperties properties;

    /**
     * ensureBucket方法用于处理ensureBucket相关的业务逻辑。
     * @return 无返回值。
     */
    @PostConstruct
    public void ensureBucket() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(properties.getBucket()).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(properties.getBucket()).build());
            }
        } catch (Exception e) {
            throw new RuntimeException("初始化 MinIO Bucket 失败", e);
        }
    }

    /**
     * upload方法用于处理upload相关的业务逻辑。
     * @param objectName objectName参数，详见调用方上下文。
     * @param inputStream inputStream参数，详见调用方上下文。
     * @param size size参数，详见调用方上下文。
     * @param contentType contentType参数，详见调用方上下文。
     * @return 返回StorageObject类型结果。
     */
    @Override
    public StorageObject upload(String objectName, InputStream inputStream, long size, String contentType) {
        try {
            PutObjectArgs.Builder builder = PutObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectName)
                    .stream(inputStream, size, -1);
            if (StringUtils.hasText(contentType)) {
                builder.contentType(contentType);
            }
            minioClient.putObject(builder.build());
            String url = buildUrl(objectName);
            return new StorageObject(objectName, url, url);
        } catch (Exception e) {
            throw new RuntimeException("上传文件至 MinIO 失败", e);
        }
    }

    /**
     * remove方法用于处理remove相关的业务逻辑。
     * @param objectUrl objectUrl参数，详见调用方上下文。
     * @return 无返回值。
     */
    @Override
    public void remove(String objectUrl) {
        String objectName = extractObjectName(objectUrl);
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("删除 MinIO 对象失败", e);
        }
    }

    /**
     * buildUrl方法用于处理buildUrl相关的业务逻辑。
     * @param objectName objectName参数，详见调用方上下文。
     * @return 返回String类型结果。
     */
    @Override
    public String buildUrl(String objectName) {
        String base = StringUtils.hasText(properties.getCdnHost())
                /**
                 * stripTrailingSlash方法用于处理stripTrailingSlash相关的业务逻辑。
                 * @param properties.getBucket( properties.getBucket(参数，详见调用方上下文。
                 * @return 返回?类型结果。
                 */
                ? stripTrailingSlash(properties.getCdnHost())
                /**
                 * stripTrailingSlash方法用于处理stripTrailingSlash相关的业务逻辑。
                 * @param properties.getBucket( properties.getBucket(参数，详见调用方上下文。
                 * @return 返回:类型结果。
                 */
                : stripTrailingSlash(properties.getEndpoint()) + "/" + properties.getBucket();
        return base + "/" + objectName;
    }

    /**
     * extractObjectName方法用于处理extractObjectName相关的业务逻辑。
     * @param objectUrl objectUrl参数，详见调用方上下文。
     * @return 返回String类型结果。
     */
    private String extractObjectName(String objectUrl) {
        if (!StringUtils.hasText(objectUrl)) {
            throw new IllegalArgumentException("objectUrl 不能为空");
        }
        if (!objectUrl.startsWith("http")) {
            return objectUrl.replaceFirst("^/", "");
        }
        String base = StringUtils.hasText(properties.getCdnHost())
                /**
                 * stripTrailingSlash方法用于处理stripTrailingSlash相关的业务逻辑。
                 * @param properties.getBucket( properties.getBucket(参数，详见调用方上下文。
                 * @return 返回?类型结果。
                 */
                ? stripTrailingSlash(properties.getCdnHost())
                /**
                 * stripTrailingSlash方法用于处理stripTrailingSlash相关的业务逻辑。
                 * @param properties.getBucket( properties.getBucket(参数，详见调用方上下文。
                 * @return 返回:类型结果。
                 */
                : stripTrailingSlash(properties.getEndpoint()) + "/" + properties.getBucket();
        String normalized = objectUrl.replaceFirst("^" + base, "");
        return normalized.replaceFirst("^/", "");
    }

    /**
     * stripTrailingSlash方法用于处理stripTrailingSlash相关的业务逻辑。
     * @param value value参数，详见调用方上下文。
     * @return 返回String类型结果。
     */
    private String stripTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}