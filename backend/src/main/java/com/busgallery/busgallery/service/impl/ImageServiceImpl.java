package com.busgallery.busgallery.service.impl;

import com.busgallery.busgallery.config.ImageAccessProperties;
import com.busgallery.busgallery.entity.Image;
import com.busgallery.busgallery.exception.BizException;
import com.busgallery.busgallery.exception.ErrorCode;
import com.busgallery.busgallery.service.ImageAccessService;
import com.busgallery.busgallery.mapper.ImageMapper;
import com.busgallery.busgallery.mapper.VehicleImageMapper;
import com.busgallery.busgallery.service.ImageService;
import com.busgallery.busgallery.service.UploadSecurityService;
import com.busgallery.busgallery.service.storage.StorageObject;
import com.busgallery.busgallery.service.storage.StorageService;
import com.busgallery.busgallery.util.ExifExtractor;
import com.busgallery.busgallery.util.ExifUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageServiceImpl implements ImageService {

    private static final DateTimeFormatter DATE_FOLDER_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final int MAX_PAGE_SIZE = 12;

    private final ImageMapper imageMapper;
    private final VehicleImageMapper vehicleImageMapper;
    private final StorageService storageService;
    private final UploadSecurityService uploadSecurityService;
    private final ImageAccessService imageAccessService;
    private final ImageAccessProperties imageAccessProperties;

    public Image findById(Long id) {
        return withSignedUrls(imageMapper.selectById(id));
    }

    public Image findRawById(Long id) {
        return imageMapper.selectById(id);
    }

    public List<Image> listByVehicle(Long vehicleId) {
        return withSignedUrls(imageMapper.selectByVehicleId(vehicleId));
    }

    public List<Image> listLatest(int limit) {
        int actual = Math.max(1, Math.min(limit <= 0 ? 10 : limit, MAX_PAGE_SIZE));
        return withSignedUrls(imageMapper.selectLatest(actual));
    }

    public List<Image> listByUploader(Long uploaderId, int page, int size) {
        if (uploaderId == null) {
            return Collections.emptyList();
        }
        int pageNo = Math.max(page, 1);
        int pageSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        int offset = (pageNo - 1) * pageSize;
        return withSignedUrls(imageMapper.selectByUploader(uploaderId, offset, pageSize));
    }

    public long countByUploader(Long uploaderId) {
        if (uploaderId == null) {
            return 0L;
        }
        return imageMapper.countByUploader(uploaderId);
    }

    public Image uploadAndSave(MultipartFile file, Image metadata) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("涓婁紶鏂囦欢涓嶈兘涓虹┖");
        }
        log.info("Uploading image: name={}, size={} bytes", file.getOriginalFilename(), file.getSize());
        Image meta = metadata != null ? metadata : new Image();
        String objectName = buildObjectName(file.getOriginalFilename());
        try {
            UploadSecurityService.ValidatedImage validated = uploadSecurityService.validateAndRead(file);
            byte[] originalData = validated.getData();
            Map<String, String> exif = ExifExtractor.extract(originalData);
            String exifJson = ExifUtils.toJson(exif);
            ProcessedUpload processed = processUploadedImage(
                    originalData,
                    validated.getMimeType(),
                    validated.getWidth(),
                    validated.getHeight()
            );
            byte[] data = processed.data();

            StorageObject storageObject;
            StorageObject thumbObject = null;
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data)) {
                storageObject = storageService.upload(
                        objectName, inputStream, data.length, processed.mimeType()
                );
            }
            byte[] thumbBytes = createThumbnail(data);
            if (thumbBytes != null && thumbBytes.length > 0) {
                String thumbObjectName = buildThumbObjectName(objectName);
                try (ByteArrayInputStream thumbStream = new ByteArrayInputStream(thumbBytes)) {
                    thumbObject = storageService.upload(
                            thumbObjectName, thumbStream, thumbBytes.length, "image/jpeg"
                    );
                }
            }

            Image image = new Image();
            image.setObjectName(storageObject.getObjectName());
            image.setUrl(storageObject.getObjectName());
            image.setThumbnailUrl(thumbObject != null ? thumbObject.getObjectName() : storageObject.getObjectName());
            image.setSizeBytes((long) data.length);
            image.setMimeType(processed.mimeType());
            image.setWidth(processed.width());
            image.setHeight(processed.height());
            image.setUploadUser(meta.getUploadUser());
            image.setUploaderId(meta.getUploaderId());
            image.setUploaderUsername(meta.getUploaderUsername());
            image.setUploaderDisplayName(meta.getUploaderDisplayName());
            image.setCreateTime(LocalDateTime.now());
            image.setExifJson(exifJson);
            imageMapper.insert(image);
            log.info("Image upload succeeded: id={}, objectName={}", image.getId(), image.getObjectName());
            return withSignedUrls(imageMapper.selectById(image.getId()));
        } catch (IOException e) {
            log.error("Image upload failed due to IO error", e);
            throw new BizException(ErrorCode.STORAGE_ERROR, "Image upload failed");
        } catch (RuntimeException e) {
            log.error("Image upload failed", e);
            throw e;
        }
    }

    public Image update(Image image) {
        imageMapper.update(image);
        return withSignedUrls(imageMapper.selectById(image.getId()));
    }

    public void delete(Long id) {
        Image image = imageMapper.selectById(id);
        if (image == null) {
            return;
        }
        vehicleImageMapper.deleteByImageId(id);
        storageService.remove(image.getObjectName());
        if (StringUtils.hasText(image.getThumbnailUrl()) && !image.getThumbnailUrl().equals(image.getObjectName())) {
            try {
                storageService.remove(image.getThumbnailUrl());
            } catch (Exception ex) {
                log.warn("Delete thumbnail failed for image {}", id, ex);
            }
        }
        imageMapper.delete(id);
    }

    private String buildObjectName(String originalFilename) {
        String extension = "";
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String folder = DATE_FOLDER_FORMATTER.format(LocalDate.now());
        return "images/" + folder + "/" + UUID.randomUUID().toString().replace("-", "") + extension;
    }

    private String buildThumbObjectName(String originalObjectName) {
        int dot = originalObjectName.lastIndexOf('.');
        if (dot > 0) {
            return originalObjectName.substring(0, dot) + "_thumb" + originalObjectName.substring(dot);
        }
        return originalObjectName + "_thumb.jpg";
    }

    private byte[] createThumbnail(byte[] data) {
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(data));
            if (source == null) {
                return null;
            }
            int maxSide = 640;
            int w = source.getWidth();
            int h = source.getHeight();
            double scale = (double) maxSide / Math.max(w, h);
            scale = Math.min(1.0, scale);
            int targetW = Math.max(1, (int) Math.round(w * scale));
            int targetH = Math.max(1, (int) Math.round(h * scale));

            BufferedImage resized = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = resized.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(source, 0, 0, targetW, targetH, null);
            g2d.dispose();

            if (imageAccessProperties.isThumbnailWatermarkEnabled()) {
                applyThumbnailWatermark(resized, imageAccessProperties.getThumbnailWatermarkText());
            }
            return writeJpegWithQuality(resized, 0.72f);
        } catch (IOException e) {
            log.warn("Create thumbnail failed, fallback to original", e);
            return null;
        }
    }

    private ProcessedUpload processUploadedImage(byte[] originalData, String mimeType, int width, int height) {
        String normalizedMimeType = normalizeMimeType(mimeType);
        int maxSide = Math.max(0, imageAccessProperties.getUploadMaxSide());
        float jpegQuality = clampQuality(imageAccessProperties.getUploadJpegQuality());
        boolean watermarkEnabled = imageAccessProperties.isUploadWatermarkEnabled()
                && StringUtils.hasText(imageAccessProperties.getUploadWatermarkText());
        boolean shouldResize = maxSide > 0 && Math.max(width, height) > maxSide;
        boolean shouldReencodeJpeg = isJpegMimeType(normalizedMimeType) && jpegQuality < 0.999f;
        if (!watermarkEnabled && !shouldResize && !shouldReencodeJpeg) {
            return new ProcessedUpload(originalData, normalizedMimeType, width, height);
        }
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(originalData));
            if (source == null) {
                return new ProcessedUpload(originalData, normalizedMimeType, width, height);
            }
            double scale = 1.0d;
            if (maxSide > 0) {
                scale = Math.min(1.0d, (double) maxSide / Math.max(source.getWidth(), source.getHeight()));
            }
            int targetW = Math.max(1, (int) Math.round(source.getWidth() * scale));
            int targetH = Math.max(1, (int) Math.round(source.getHeight() * scale));
            int imageType = "image/png".equals(normalizedMimeType) ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
            BufferedImage processed = new BufferedImage(targetW, targetH, imageType);
            Graphics2D g2d = processed.createGraphics();
            try {
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.drawImage(source, 0, 0, targetW, targetH, null);
            } finally {
                g2d.dispose();
            }
            if (watermarkEnabled) {
                applyWatermark(processed, imageAccessProperties.getUploadWatermarkText(), 0.16f);
            }
            byte[] bytes;
            if ("image/png".equals(normalizedMimeType)) {
                bytes = writePng(processed);
            } else {
                bytes = writeJpegWithQuality(processed, jpegQuality);
            }
            return new ProcessedUpload(bytes, normalizedMimeType, targetW, targetH);
        } catch (IOException e) {
            log.warn("Post-processing upload image failed, fallback to original data", e);
            return new ProcessedUpload(originalData, normalizedMimeType, width, height);
        }
    }

    private String normalizeMimeType(String mimeType) {
        if (!StringUtils.hasText(mimeType)) {
            return "image/jpeg";
        }
        String normalized = mimeType.trim().toLowerCase();
        if ("image/png".equals(normalized)) {
            return "image/png";
        }
        if ("image/jpeg".equals(normalized) || "image/jpg".equals(normalized)) {
            return "image/jpeg";
        }
        return "image/jpeg";
    }

    private boolean isJpegMimeType(String mimeType) {
        return "image/jpeg".equals(mimeType) || "image/jpg".equals(mimeType);
    }

    private float clampQuality(float quality) {
        return Math.max(0.1f, Math.min(1.0f, quality));
    }

    private void applyThumbnailWatermark(BufferedImage image, String watermarkText) {
        applyWatermark(image, watermarkText, 0.2f);
    }

    private void applyWatermark(BufferedImage image, String watermarkText, float alpha) {
        if (image == null || !StringUtils.hasText(watermarkText)) {
            return;
        }
        Graphics2D g2 = image.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int minSide = Math.min(image.getWidth(), image.getHeight());
            int fontSize = Math.max(14, minSide / 16);
            g2.setFont(new Font("SansSerif", Font.BOLD, fontSize));
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.05f, Math.min(0.7f, alpha))));
            g2.setColor(Color.WHITE);

            String text = watermarkText.trim();
            int stepX = Math.max(120, image.getWidth() / 2);
            int stepY = Math.max(90, image.getHeight() / 3);
            int baseline = stepY;
            for (int y = baseline; y < image.getHeight() + stepY; y += stepY) {
                for (int x = -stepX / 2; x < image.getWidth() + stepX; x += stepX) {
                    g2.drawString(text, x, y);
                }
            }
        } finally {
            g2.dispose();
        }
    }

    private byte[] writePng(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    private byte[] writeJpegWithQuality(BufferedImage image, float quality) throws IOException {
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

    private Image withSignedUrls(Image source) {
        return imageAccessService.toSignedImage(source);
    }

    private List<Image> withSignedUrls(List<Image> list) {
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        return list.stream()
                .map(this::withSignedUrls)
                .toList();
    }

    private record ProcessedUpload(byte[] data, String mimeType, int width, int height) {
    }
}

