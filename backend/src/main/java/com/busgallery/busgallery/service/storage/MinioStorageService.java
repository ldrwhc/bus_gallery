package com.busgallery.busgallery.service.storage;

import com.busgallery.busgallery.exception.BizException;
import com.busgallery.busgallery.exception.ErrorCode;
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
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

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
            throw new BizException(ErrorCode.STORAGE_ERROR, "Init MinIO bucket failed");
        }
    }

    @Override
    public StorageObject upload(String objectName, InputStream inputStream, long size, String contentType) {
        int attempts = 0;
        int maxAttempts = 2;
        long backoffMs = 200;
        while (true) {
            try {
                if (attempts > 0 && inputStream.markSupported()) {
                    inputStream.reset();
                } else if (attempts > 0) {
                    throw new BizException(ErrorCode.STORAGE_ERROR, "Upload stream is not repeatable");
                }
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
                attempts++;
                if (isConnectionIssue(e)) {
                    throw new BizException(ErrorCode.STORAGE_ERROR, "MinIO unreachable or timed out, please check service and endpoint");
                }
                if (attempts >= maxAttempts) {
                    throw new BizException(ErrorCode.STORAGE_ERROR, "Upload to MinIO failed after retries");
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(backoffMs);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw new BizException(ErrorCode.STORAGE_ERROR, "Upload to MinIO interrupted");
                }
                backoffMs = Math.min(backoffMs * 2, 1000);
            }
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
            throw new BizException(ErrorCode.STORAGE_ERROR, "Delete MinIO object failed");
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
            throw new IllegalArgumentException("objectUrl cannot be empty");
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

    private boolean isConnectionIssue(Exception e) {
        Throwable current = e;
        while (current != null) {
            if (current instanceof UnknownHostException
                    || current instanceof ConnectException
                    || current instanceof SocketTimeoutException
                    || current instanceof NoRouteToHostException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
