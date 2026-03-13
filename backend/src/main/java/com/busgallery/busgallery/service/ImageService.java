package com.busgallery.busgallery.service;

import com.busgallery.busgallery.entity.Image;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ImageService {

    Image findById(Long id);

    List<Image> listByVehicle(Long vehicleId);

    List<Image> listLatest(int limit);

    List<Image> listByUploader(Long uploaderId, int page, int size);

    long countByUploader(Long uploaderId);

    Image uploadAndSave(MultipartFile file, Image metadata);

    Image update(Image image);

    void delete(Long id);
}
