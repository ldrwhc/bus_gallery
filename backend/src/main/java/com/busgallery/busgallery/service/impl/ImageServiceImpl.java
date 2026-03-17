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
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    private static final DateTimeFormatter DATE_FOLDER_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final ImageMapper imageMapper;
    private final VehicleImageMapper vehicleImageMapper;
    private final StorageService storageService;

    @Override
    public Image findById(Long id) {
        return imageMapper.selectById(id);
    }

    @Override
    public List<Image> listByVehicle(Long vehicleId) {
        return imageMapper.selectByVehicleId(vehicleId);
    }

    @Override
    public List<Image> listLatest(int limit) {
        int actual = limit <= 0 ? 10 : limit;
        return imageMapper.selectLatest(actual);
    }

    @Override
    public List<Image> listByUploader(Long uploaderId, int page, int size) {
        if (uploaderId == null) {
            return Collections.emptyList();
        }
        int pageNo = Math.max(page, 1);
        int pageSize = Math.max(size, 1);
        int offset = (pageNo - 1) * pageSize;
        return imageMapper.selectByUploader(uploaderId, offset, pageSize);
    }

    @Override
    public long countByUploader(Long uploaderId) {
        if (uploaderId == null) {
            return 0L;
        }
        return imageMapper.countByUploader(uploaderId);
    }

    @Override
    @Transactional
    public Image uploadAndSave(MultipartFile file, Image metadata) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        Image meta = metadata != null ? metadata : new Image();
        String objectName = buildObjectName(file.getOriginalFilename());
        try {
            byte[] data = file.getBytes();
            Map<String, String> exif = ExifExtractor.extract(data);
            String exifJson = ExifUtils.toJson(exif);

            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data)) {
                StorageObject storageObject = storageService.upload(
                        objectName, inputStream, file.getSize(), file.getContentType()
                );
                Image image = new Image();
                image.setObjectName(storageObject.getObjectName());
                image.setUrl(storageObject.getUrl());
                image.setThumbnailUrl(storageObject.getThumbnailUrl());
                image.setSizeBytes(file.getSize());
                image.setMimeType(file.getContentType());
                image.setUploadUser(meta.getUploadUser());
                image.setUploaderId(meta.getUploaderId());
                image.setUploaderUsername(meta.getUploaderUsername());
                image.setUploaderDisplayName(meta.getUploaderDisplayName());
                image.setCreateTime(LocalDateTime.now());
                image.setExifJson(exifJson);
                imageMapper.insert(image);
                return imageMapper.selectById(image.getId());
            }
        } catch (IOException e) {
            log.error("Image upload failed due to IO error", e);
            throw new BizException(ErrorCode.STORAGE_ERROR, "Image upload failed");
        } catch (RuntimeException e) {
            log.error("Image upload failed", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public Image update(Image image) {
        imageMapper.update(image);
        return imageMapper.selectById(image.getId());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Image image = imageMapper.selectById(id);
        if (image == null) {
            return;
        }
        vehicleImageMapper.deleteByImageId(id);
        storageService.remove(image.getObjectName());
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
}
