package com.busgallery.busgallery.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "busgallery.image-access")
public class ImageAccessProperties {

    /**
     * HMAC secret for signed image access tokens.
     */
    private String tokenSecret = "change-me-image-access-secret";

    /**
     * Signed URL ttl for full-size images.
     */
    private int fullTtlSeconds = 300;

    /**
     * Signed URL ttl for thumbnails.
     */
    private int thumbnailTtlSeconds = 900;

    /**
     * Whether to add watermark to generated thumbnails on upload.
     */
    private boolean thumbnailWatermarkEnabled = true;

    /**
     * Watermark text used for generated thumbnails.
     */
    private String thumbnailWatermarkText = "BUS GALLERY";
}

