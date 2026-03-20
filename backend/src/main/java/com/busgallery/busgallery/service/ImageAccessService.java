package com.busgallery.busgallery.service;

import com.busgallery.busgallery.config.ImageAccessProperties;
import com.busgallery.busgallery.entity.Image;
import com.busgallery.busgallery.exception.BizException;
import com.busgallery.busgallery.exception.ErrorCode;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class ImageAccessService {

    private static final String HMAC_ALGO = "HmacSHA256";

    private final MinioClient minioClient;
    private final com.busgallery.busgallery.service.storage.StorageProperties storageProperties;
    private final ImageAccessProperties properties;

    public Image toSignedImage(Image source) {
        if (source == null) {
            return null;
        }
        Image copy = new Image();
        BeanUtils.copyProperties(source, copy);
        String primaryObjectName = resolvePrimaryObjectName(source);
        if (!StringUtils.hasText(primaryObjectName)) {
            return copy;
        }
        copy.setUrl(buildSignedAccessUrl(primaryObjectName, properties.getFullTtlSeconds()));
        String thumbObjectName = resolveThumbnailObjectName(source);
        copy.setThumbnailUrl(StringUtils.hasText(thumbObjectName)
                ? buildSignedAccessUrl(thumbObjectName, properties.getThumbnailTtlSeconds())
                : copy.getUrl());
        return copy;
    }

    public SignedImageStream resolveStream(String token) {
        String normalized = StringUtils.hasText(token) ? token.trim() : "";
        int dot = normalized.lastIndexOf('.');
        if (dot <= 0 || dot >= normalized.length() - 1) {
            throw new BizException(ErrorCode.INVALID_PARAM, "Invalid image access token");
        }
        String payload = normalized.substring(0, dot);
        String signature = normalized.substring(dot + 1);
        String expected = hmacHex(payload);
        if (!expected.equalsIgnoreCase(signature)) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "Image token signature mismatch");
        }

        String decoded;
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(payload);
            decoded = new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new BizException(ErrorCode.INVALID_PARAM, "Invalid image token payload");
        }

        String[] parts = decoded.split("\\|", 2);
        if (parts.length < 2) {
            throw new BizException(ErrorCode.INVALID_PARAM, "Invalid image token content");
        }
        String objectName = normalizeObjectName(parts[0]);
        long expiresAt;
        try {
            expiresAt = Long.parseLong(parts[1]);
        } catch (NumberFormatException ex) {
            throw new BizException(ErrorCode.INVALID_PARAM, "Invalid image token expiration");
        }
        if (Instant.now().getEpochSecond() > expiresAt) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "Image token expired");
        }

        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(storageProperties.getBucket())
                            .object(objectName)
                            .build()
            );
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(storageProperties.getBucket())
                            .object(objectName)
                            .build()
            );
            String contentType = StringUtils.hasText(stat.contentType()) ? stat.contentType() : "application/octet-stream";
            return new SignedImageStream(stream, contentType, stat.size());
        } catch (ErrorResponseException ex) {
            if (ex.errorResponse() != null && "NoSuchKey".equalsIgnoreCase(ex.errorResponse().code())) {
                throw new BizException(ErrorCode.NOT_FOUND, "Image object not found");
            }
            throw new BizException(ErrorCode.STORAGE_ERROR, "Open image object failed");
        } catch (Exception ex) {
            throw new BizException(ErrorCode.STORAGE_ERROR, "Open image object failed");
        }
    }

    public String resolveThumbnailObjectName(Image image) {
        if (image == null) {
            return null;
        }
        String thumbnailRef = image.getThumbnailUrl();
        if (StringUtils.hasText(thumbnailRef)) {
            String fromThumb = tryResolveObjectName(thumbnailRef);
            if (StringUtils.hasText(fromThumb)) {
                return fromThumb;
            }
        }
        String objectName = normalizeObjectName(image.getObjectName());
        if (!StringUtils.hasText(objectName)) {
            objectName = resolvePrimaryObjectName(image);
        }
        return StringUtils.hasText(objectName) ? objectName : null;
    }

    private String resolvePrimaryObjectName(Image image) {
        if (image == null) {
            return "";
        }
        String fromObjectName = normalizeObjectName(image.getObjectName());
        if (StringUtils.hasText(fromObjectName)) {
            return fromObjectName;
        }
        return normalizeObjectName(image.getUrl());
    }

    private String buildSignedAccessUrl(String objectName, int ttlSeconds) {
        String normalized = normalizeObjectName(objectName);
        if (!StringUtils.hasText(normalized)) {
            throw new BizException(ErrorCode.INVALID_PARAM, "Invalid image object name");
        }
        int safeTtl = Math.max(30, ttlSeconds);
        long expiresAt = Instant.now().getEpochSecond() + safeTtl;
        String plain = normalized + "|" + expiresAt;
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(plain.getBytes(StandardCharsets.UTF_8));
        String signature = hmacHex(payload);
        return "/api/images/access/" + payload + "." + signature;
    }

    private String tryResolveObjectName(String ref) {
        String normalized = normalizeObjectName(ref);
        if (StringUtils.hasText(normalized)) {
            return normalized;
        }
        return null;
    }

    private String normalizeObjectName(String ref) {
        if (!StringUtils.hasText(ref)) {
            return "";
        }
        String value = ref.trim();
        if (!value.startsWith("http")) {
            String direct = stripLeadingSlash(value);
            String bucketPrefix = storageProperties.getBucket() + "/";
            if (direct.startsWith(bucketPrefix)) {
                direct = direct.substring(bucketPrefix.length());
            }
            int idx = direct.indexOf("images/");
            if (idx > 0) {
                direct = direct.substring(idx);
            }
            if (direct.contains("..")) {
                return "";
            }
            return direct;
        }
        try {
            URI uri = URI.create(value);
            String path = uri.getPath();
            if (!StringUtils.hasText(path)) {
                return "";
            }
            String normalizedPath = stripLeadingSlash(path);
            String bucketPrefix = storageProperties.getBucket() + "/";
            if (normalizedPath.startsWith(bucketPrefix)) {
                String object = normalizedPath.substring(bucketPrefix.length());
                return object.contains("..") ? "" : object;
            }
            int idx = normalizedPath.indexOf("images/");
            if (idx >= 0) {
                String object = normalizedPath.substring(idx);
                return object.contains("..") ? "" : object;
            }
            return normalizedPath.contains("..") ? "" : normalizedPath;
        } catch (Exception ex) {
            return "";
        }
    }

    private String stripLeadingSlash(String value) {
        String result = value;
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        return result;
    }

    private String hmacHex(String text) {
        String secret = StringUtils.hasText(properties.getTokenSecret())
                ? properties.getTokenSecret()
                : "change-me-image-access-secret";
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            byte[] hash = mac.doFinal(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new BizException(ErrorCode.STORAGE_ERROR, "Sign image url failed");
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class SignedImageStream {
        private final InputStream inputStream;
        private final String contentType;
        private final long contentLength;
    }
}
