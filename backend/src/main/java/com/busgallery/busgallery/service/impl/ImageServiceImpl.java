package com.busgallery.busgallery.service.impl;

import com.busgallery.busgallery.entity.Image;
import com.busgallery.busgallery.mapper.ImageMapper;
import com.busgallery.busgallery.mapper.VehicleImageMapper;
import com.busgallery.busgallery.service.ImageService;
import com.busgallery.busgallery.service.storage.StorageObject;
import com.busgallery.busgallery.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.busgallery.busgallery.util.ExifExtractor;
import com.busgallery.busgallery.util.ExifUtils;

/**
 * ImageServiceImpl类用于封装ImageServiceImpl相关的领域职责（所在包：com.busgallery.busgallery.service.impl）。
 */
@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    private static final DateTimeFormatter DATE_FOLDER_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final ImageMapper imageMapper;
    private final VehicleImageMapper vehicleImageMapper;
    private final StorageService storageService;

    /**
     * findById方法用于处理findById相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 返回Image类型结果。
     */
    @Override
    public Image findById(Long id) {
        return imageMapper.selectById(id);
    }

    /**
     * listByVehicle方法用于处理listByVehicle相关的业务逻辑。
     * @param vehicleId vehicleId参数，详见调用方上下文。
     * @return 返回List<Image>类型结果。
     */
    @Override
    public List<Image> listByVehicle(Long vehicleId) {
        return imageMapper.selectByVehicleId(vehicleId);
    }

    /**
     * listLatest方法用于处理listLatest相关的业务逻辑。
     * @param limit limit参数，详见调用方上下文。
     * @return 返回List<Image>类型结果。
     */
    @Override
    public List<Image> listLatest(int limit) {
        int actual = limit <= 0 ? 10 : limit;
        return imageMapper.selectLatest(actual);
    }

    /**
     * listByUploader方法用于处理listByUploader相关的业务逻辑。
     * @param uploaderId uploaderId参数，详见调用方上下文。
     * @param page page参数，详见调用方上下文。
     * @param size size参数，详见调用方上下文。
     * @return 返回List<Image>类型结果。
     */
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

    /**
     * countByUploader方法用于处理countByUploader相关的业务逻辑。
     * @param uploaderId uploaderId参数，详见调用方上下文。
     * @return 返回long类型结果。
     */
    @Override
    public long countByUploader(Long uploaderId) {
        if (uploaderId == null) {
            return 0L;
        }
        return imageMapper.countByUploader(uploaderId);
    }

    /**
     * uploadAndSave方法用于处理uploadAndSave相关的业务逻辑。
     * @param file file参数，详见调用方上下文。
     * @param metadata metadata参数，详见调用方上下文。
     * @return 返回Image类型结果。
     */
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
            throw new RuntimeException("图片上传失败", e);
        }
    }

    /**
     * update方法用于处理update相关的业务逻辑。
     * @param image image参数，详见调用方上下文。
     * @return 返回Image类型结果。
     */
    @Override
    @Transactional
    public Image update(Image image) {
        imageMapper.update(image);
        return imageMapper.selectById(image.getId());
    }

    /**
     * delete方法用于处理delete相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 无返回值。
     */
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

    /**
     * buildObjectName方法用于处理buildObjectName相关的业务逻辑。
     * @param originalFilename originalFilename参数，详见调用方上下文。
     * @return 返回String类型结果。
     */
    private String buildObjectName(String originalFilename) {
        String extension = "";
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String folder = DATE_FOLDER_FORMATTER.format(LocalDate.now());
        return "images/" + folder + "/" + UUID.randomUUID().toString().replace("-", "") + extension;
    }
}
