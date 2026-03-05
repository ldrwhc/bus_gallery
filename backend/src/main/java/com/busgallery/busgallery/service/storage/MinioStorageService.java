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

@Service
@Profile("!test")
@RequiredArgsConstructor
public class MinioStorageService implements StorageService {

    private final MinioClient minioClient;
    private final StorageProperties properties;

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

    @Override
    public String buildUrl(String objectName) {
        String base = StringUtils.hasText(properties.getCdnHost())
                ? stripTrailingSlash(properties.getCdnHost())
                : stripTrailingSlash(properties.getEndpoint()) + "/" + properties.getBucket();
        return base + "/" + objectName;
    }

    private String extractObjectName(String objectUrl) {
        if (!StringUtils.hasText(objectUrl)) {
            throw new IllegalArgumentException("objectUrl 不能为空");
        }
        if (!objectUrl.startsWith("http")) {
            return objectUrl.replaceFirst("^/", "");
        }
        String base = StringUtils.hasText(properties.getCdnHost())
                ? stripTrailingSlash(properties.getCdnHost())
                : stripTrailingSlash(properties.getEndpoint()) + "/" + properties.getBucket();
        String normalized = objectUrl.replaceFirst("^" + base, "");
        return normalized.replaceFirst("^/", "");
    }

    private String stripTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}