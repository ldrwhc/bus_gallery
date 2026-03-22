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
    private int fullTtlSeconds = 7200;

    /**
     * Signed URL ttl for thumbnails.
     */
    private int thumbnailTtlSeconds = 21600;

    /**
     * Whether to add watermark to generated thumbnails on upload.
     */
    private boolean thumbnailWatermarkEnabled = true;

    /**
     * Watermark text used for generated thumbnails.
     */
    private String thumbnailWatermarkText = "BUS GALLERY";

    /**
     * Whether to add watermark to uploaded full-size images.
     */
    private boolean uploadWatermarkEnabled = false;

    /**
     * Watermark text used for uploaded full-size images.
     */
    private String uploadWatermarkText = "BUS GALLERY";

    /**
     * JPEG quality used when re-encoding uploaded full-size JPEG images.
     */
    private float uploadJpegQuality = 0.86f;

    /**
     * Max width/height of uploaded full-size images. 0 means no resize.
     */
    private int uploadMaxSide = 2560;
}
