package com.busgallery.busgallery.job;

import com.busgallery.busgallery.config.ImageAccessProperties;
import com.busgallery.busgallery.entity.Image;
import com.busgallery.busgallery.mapper.ImageMapper;
import com.busgallery.busgallery.service.ImageAccessService;
import com.busgallery.busgallery.service.storage.StorageProperties;
import com.busgallery.busgallery.service.storage.StorageService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

@Component
@ConditionalOnProperty(name = "busgallery.image-display-backfill.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class ImageDisplayBackfillRunner implements CommandLineRunner {

    private final ImageMapper imageMapper;
    private final StorageService storageService;
    private final ImageAccessService imageAccessService;
    private final MinioClient minioClient;
    private final StorageProperties storageProperties;
    private final ImageAccessProperties imageAccessProperties;

    @Value("${busgallery.image-display-backfill.limit:0}")
    private int limit;

    @Value("${busgallery.image-display-backfill.force-regenerate:false}")
    private boolean forceRegenerate;

    @Value("${busgallery.image-display-backfill.max-side:1280}")
    private int maxSide;

    @Value("${busgallery.image-display-backfill.jpeg-quality:0.8}")
    private float jpegQuality;

    @Value("${busgallery.image-display-backfill.watermark-alpha:0.35}")
    private float watermarkAlpha;

    @Value("${busgallery.image-display-backfill.watermark-text:}")
    private String watermarkText;

    @Override
    public void run(String... args) {
        log.info("Image display backfill started. forceRegenerate={}, limit={}", forceRegenerate, limit);
        List<Image> images = imageMapper.selectAll();
        int scanned = 0;
        int uploaded = 0;
        int dbUpdated = 0;
        int skipped = 0;
        int failed = 0;

        for (Image image : images) {
            if (limit > 0 && scanned >= limit) {
                break;
            }
            scanned++;
            try {
                BackfillResult result = processOne(image);
                if (result.uploaded()) {
                    uploaded++;
                }
                if (result.dbUpdated()) {
                    dbUpdated++;
                }
                if (!result.uploaded() && !result.dbUpdated()) {
                    skipped++;
                }
            } catch (Exception ex) {
                failed++;
                log.warn("Image display backfill failed for imageId={}", image.getId(), ex);
            }
        }

        log.info(
                "Image display backfill finished. scanned={}, uploaded={}, dbUpdated={}, skipped={}, failed={}",
                scanned, uploaded, dbUpdated, skipped, failed
        );
    }

    private BackfillResult processOne(Image image) throws Exception {
        if (image == null || image.getId() == null) {
            return BackfillResult.NONE;
        }

        String originalObject = imageAccessService.resolveObjectNameRef(image.getObjectName());
        if (!StringUtils.hasText(originalObject)) {
            return BackfillResult.NONE;
        }

        String displayObject = buildDisplayObjectName(originalObject);
        String currentPrimary = imageAccessService.resolveObjectNameRef(image.getUrl());
        boolean displayExists = imageAccessService.objectExistsRef(displayObject);

        boolean uploaded = false;
        if (forceRegenerate || !displayExists) {
            byte[] sourceBytes = readObjectBytes(originalObject);
            if (sourceBytes == null || sourceBytes.length == 0) {
                String fallback = StringUtils.hasText(currentPrimary) ? currentPrimary : imageAccessService.resolveObjectNameRef(image.getThumbnailUrl());
                if (StringUtils.hasText(fallback)) {
                    sourceBytes = readObjectBytes(fallback);
                }
            }
            if (sourceBytes == null || sourceBytes.length == 0) {
                throw new IllegalStateException("Source object bytes not found for imageId=" + image.getId());
            }

            byte[] displayBytes = createDisplayImage(sourceBytes);
            if (displayBytes == null || displayBytes.length == 0) {
                throw new IllegalStateException("Display image generation failed for imageId=" + image.getId());
            }
            storageService.upload(displayObject, new ByteArrayInputStream(displayBytes), displayBytes.length, "image/jpeg");
            uploaded = true;
        }

        boolean dbUpdated = false;
        if (!displayObject.equals(currentPrimary)) {
            image.setUrl(displayObject);
            imageMapper.update(image);
            dbUpdated = true;
        }
        return new BackfillResult(uploaded, dbUpdated);
    }

    private byte[] readObjectBytes(String objectName) throws Exception {
        if (!StringUtils.hasText(objectName)) {
            return null;
        }
        try (InputStream inputStream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(storageProperties.getBucket())
                        .object(objectName)
                        .build()
        )) {
            return inputStream.readAllBytes();
        } catch (ErrorResponseException ex) {
            if (ex.errorResponse() != null && "NoSuchKey".equalsIgnoreCase(ex.errorResponse().code())) {
                return null;
            }
            throw ex;
        }
    }

    private String buildDisplayObjectName(String originalObjectName) {
        int dot = originalObjectName.lastIndexOf('.');
        if (dot > 0) {
            return originalObjectName.substring(0, dot) + "_display.jpg";
        }
        return originalObjectName + "_display.jpg";
    }

    private byte[] createDisplayImage(byte[] data) throws Exception {
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(data));
        if (source == null) {
            return null;
        }
        int safeMaxSide = Math.max(1, maxSide);
        int width = source.getWidth();
        int height = source.getHeight();
        double scale = (double) safeMaxSide / Math.max(width, height);
        scale = Math.min(1.0d, scale);
        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));

        BufferedImage preview = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = preview.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, targetWidth, targetHeight);
            g2d.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            g2d.dispose();
        }

        applyWatermark(preview, resolveWatermarkText(), watermarkAlpha);
        return writeJpeg(preview, jpegQuality);
    }

    private void applyWatermark(BufferedImage image, String text, float alpha) {
        if (image == null || !StringUtils.hasText(text)) {
            return;
        }
        Graphics2D g2d = image.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(Color.WHITE);
            int minSide = Math.min(image.getWidth(), image.getHeight());
            int fontSize = Math.max(16, minSide / 14);
            g2d.setFont(new Font("SansSerif", Font.BOLD, fontSize));
            g2d.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER,
                    Math.max(0.05f, Math.min(0.7f, alpha))
            ));

            int stepX = Math.max(140, image.getWidth() / 2);
            int stepY = Math.max(100, image.getHeight() / 3);
            int baseline = stepY;
            for (int y = baseline; y < image.getHeight() + stepY; y += stepY) {
                for (int x = -stepX / 2; x < image.getWidth() + stepX; x += stepX) {
                    g2d.drawString(text, x, y);
                }
            }
        } finally {
            g2d.dispose();
        }
    }

    private String resolveWatermarkText() {
        if (StringUtils.hasText(watermarkText)) {
            return watermarkText.trim();
        }
        if (StringUtils.hasText(imageAccessProperties.getUploadWatermarkText())) {
            return imageAccessProperties.getUploadWatermarkText().trim();
        }
        if (StringUtils.hasText(imageAccessProperties.getThumbnailWatermarkText())) {
            return imageAccessProperties.getThumbnailWatermarkText().trim();
        }
        return "BUS GALLERY";
    }

    private byte[] writeJpeg(BufferedImage image, float quality) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        var writers = ImageIO.getImageWritersByFormatName("jpg");
        if (writers.hasNext()) {
            var writer = writers.next();
            var ios = ImageIO.createImageOutputStream(baos);
            try {
                writer.setOutput(ios);
                var params = writer.getDefaultWriteParam();
                if (params.canWriteCompressed()) {
                    params.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
                    params.setCompressionQuality(Math.max(0.1f, Math.min(1.0f, quality)));
                }
                writer.write(null, new javax.imageio.IIOImage(image, null, null), params);
            } finally {
                ios.close();
                writer.dispose();
            }
        } else {
            ImageIO.write(image, "jpg", baos);
        }
        return baos.toByteArray();
    }

    private record BackfillResult(boolean uploaded, boolean dbUpdated) {
        private static final BackfillResult NONE = new BackfillResult(false, false);
    }
}
