package com.busgallery.busgallery.job;

import com.busgallery.busgallery.entity.Image;
import com.busgallery.busgallery.mapper.ImageMapper;
import com.busgallery.busgallery.service.storage.StorageObject;
import com.busgallery.busgallery.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * ThumbnailRebuildRunner 类。
 */
@Component
@ConditionalOnProperty(name = "busgallery.thumbnails.rebuild", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class ThumbnailRebuildRunner implements CommandLineRunner {

    private final ImageMapper imageMapper;
    private final StorageService storageService;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * run 鏂规硶銆?
     * @param args 鍙傛暟
     */
    public void run(String... args) throws Exception {
        log.info("Thumbnail rebuild started...");
        List<Image> images = imageMapper.selectAll();
        int updated = 0;
        for (Image img : images) {
            if (StringUtils.hasText(img.getThumbnailUrl()) && !img.getThumbnailUrl().equals(img.getUrl())) {
                continue; // already has dedicated thumbnail
            }
            if (!StringUtils.hasText(img.getUrl())) {
                continue;
            }
            try {
                byte[] original = download(img.getUrl());
                byte[] thumb = createThumbnail(original);
                if (thumb == null || thumb.length == 0) {
                    continue;
                }
                String thumbObjectName = buildThumbObjectName(img.getObjectName());
                StorageObject stored = storageService.upload(thumbObjectName,
                        new ByteArrayInputStream(thumb), thumb.length, "image/jpeg");
                img.setThumbnailUrl(stored.getUrl());
                imageMapper.update(img);
                updated++;
            } catch (Exception e) {
                log.warn("Failed to rebuild thumbnail for image {}", img.getId(), e);
            }
        }
        log.info("Thumbnail rebuild finished. updated={}/{}", updated, images.size());
    }

    private byte[] download(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(20)).build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        return response.body();
    }

    private String buildThumbObjectName(String originalObjectName) {
        if (!StringUtils.hasText(originalObjectName)) {
            return "thumbs/" + System.currentTimeMillis() + ".jpg";
        }
        int dot = originalObjectName.lastIndexOf('.');
        if (dot > 0) {
            return originalObjectName.substring(0, dot) + "_thumb" + originalObjectName.substring(dot);
        }
        return originalObjectName + "_thumb.jpg";
    }

    private byte[] createThumbnail(byte[] data) {
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(data));
            if (source == null) return null;
            int maxSide = 640;
            int w = source.getWidth();
            int h = source.getHeight();
            double scale = Math.min(1.0, (double) maxSide / Math.max(w, h));
            int targetW = Math.max(1, (int) Math.round(w * scale));
            int targetH = Math.max(1, (int) Math.round(h * scale));
            BufferedImage resized = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = resized.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(source, 0, 0, targetW, targetH, null);
            g2d.dispose();
            return writeJpeg(resized, 0.72f);
        } catch (Exception e) {
            log.warn("Create thumb failed", e);
            return null;
        }
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
}
