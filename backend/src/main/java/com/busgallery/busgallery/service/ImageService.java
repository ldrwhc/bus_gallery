package com.busgallery.busgallery.service;

import com.busgallery.busgallery.entity.Image;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ImageService {

    Image findById(Long id);

    List<Image> listByVehicle(Long vehicleId);

    List<Image> listLatest(int limit);

    Image uploadAndSave(MultipartFile file, String uploadUser);

    Image update(Image image);

    void delete(Long id);
}