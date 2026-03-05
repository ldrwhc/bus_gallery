package com.busgallery.busgallery.controller;

import com.busgallery.busgallery.entity.Image;
import com.busgallery.busgallery.service.ImageService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @GetMapping("/{id}")
    public Image detail(@PathVariable Long id) {
        return imageService.findById(id);
    }

    @GetMapping("/latest")
    public List<Image> latest(@RequestParam(defaultValue = "12") int limit) {
        return imageService.listLatest(limit);
    }

    @GetMapping("/vehicle/{vehicleId}")
    public List<Image> listByVehicle(@PathVariable Long vehicleId) {
        return imageService.listByVehicle(vehicleId);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Image upload(@RequestPart("file") MultipartFile file,
                        @RequestParam(value = "uploadUser", required = false) String uploadUser) {
        return imageService.uploadAndSave(file, uploadUser);
    }

    @PutMapping("/{id}")
    public Image update(@PathVariable Long id, @RequestBody ImageUpdateRequest request) {
        Image image = imageService.findById(id);
        if (image == null) {
            return null;
        }
        image.setUploadUser(request.getUploadUser());
        return imageService.update(image);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        imageService.delete(id);
    }

    @Data
    public static class ImageUpdateRequest {
        private String uploadUser;
    }
}