package com.busgallery.busgallery.service.impl;

import com.busgallery.busgallery.entity.Image;
import com.busgallery.busgallery.exception.BizException;
import com.busgallery.busgallery.exception.ErrorCode;
import com.busgallery.busgallery.mapper.ImageMapper;
import com.busgallery.busgallery.mapper.VehicleImageMapper;
import com.busgallery.busgallery.service.ImageService;
import com.busgallery.busgallery.service.storage.StorageObject;
import com.busgallery.busgallery.service.storage.StorageService;
import com.busgallery.busgallery.util.ExifExtractor;
import com.busgallery.busgallery.util.ExifUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

    private final ImageMapper imageMapper;
    private final VehicleImageMapper vehicleImageMapper;
    private final StorageService storageService;

    public Image findById(Long id) {
        return imageMapper.selectById(id);
    }

    public List<Image> listByVehicle(Long vehicleId) {
        return imageMapper.selectByVehicleId(vehicleId);
    }

    public List<Image> listLatest(int limit) {
        int actual = limit <= 0 ? 10 : limit;
        return imageMapper.selectLatest(actual);
    }

    public List<Image> listByUploader(Long uploaderId, int page, int size) {
        if (uploaderId == null) {
            return Collections.emptyList();
        }
        int pageNo = Math.max(page, 1);
        int pageSize = Math.max(size, 1);
        int offset = (pageNo - 1) * pageSize;
        return imageMapper.selectByUploader(uploaderId, offset, pageSize);
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
            byte[] data = file.getBytes();
            Map<String, String> exif = ExifExtractor.extract(data);
            String exifJson = ExifUtils.toJson(exif);

            StorageObject storageObject;
            StorageObject thumbObject = null;
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data)) {
                storageObject = storageService.upload(
                        objectName, inputStream, file.getSize(), file.getContentType()
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
            image.setUrl(storageObject.getUrl());
            image.setThumbnailUrl(thumbObject != null ? thumbObject.getUrl() : storageObject.getThumbnailUrl());
            image.setSizeBytes(file.getSize());
            image.setMimeType(file.getContentType());
            image.setUploadUser(meta.getUploadUser());
            image.setUploaderId(meta.getUploaderId());
            image.setUploaderUsername(meta.getUploaderUsername());
            image.setUploaderDisplayName(meta.getUploaderDisplayName());
            image.setCreateTime(LocalDateTime.now());
            image.setExifJson(exifJson);
            imageMapper.insert(image);
            log.info("Image upload succeeded: id={}, objectName={}", image.getId(), image.getObjectName());
            return imageMapper.selectById(image.getId());
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
        return imageMapper.selectById(image.getId());
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

            return writeJpegWithQuality(resized, 0.72f);
        } catch (IOException e) {
            log.warn("Create thumbnail failed, fallback to original", e);
            return null;
        }
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
}

