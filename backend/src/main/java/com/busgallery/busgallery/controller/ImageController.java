package com.busgallery.busgallery.controller;

import com.busgallery.busgallery.auth.AuthContextHolder;
import com.busgallery.busgallery.auth.RequireLogin;
import com.busgallery.busgallery.auth.UserSession;
import com.busgallery.busgallery.entity.Image;
import com.busgallery.busgallery.service.ImageService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * ImageController类用于封装ImageController相关的领域职责（所在包：com.busgallery.busgallery.controller）。
 */
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    /**
     * detail方法用于处理detail相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 返回Image类型结果。
     */
    @GetMapping("/{id}")
    public Image detail(@PathVariable Long id) {
        return imageService.findById(id);
    }

    /**
     * latest方法用于处理latest相关的业务逻辑。
     * @param limit limit参数，详见调用方上下文。
     * @return 返回List<Image>类型结果。
     */
    @GetMapping("/latest")
    public List<Image> latest(@RequestParam(defaultValue = "12") int limit) {
        return imageService.listLatest(limit);
    }

    /**
     * listByVehicle方法用于处理listByVehicle相关的业务逻辑。
     * @param vehicleId vehicleId参数，详见调用方上下文。
     * @return 返回List<Image>类型结果。
     */
    @GetMapping("/vehicle/{vehicleId}")
    public List<Image> listByVehicle(@PathVariable Long vehicleId) {
        return imageService.listByVehicle(vehicleId);
    }

    /**
     * upload方法用于处理upload相关的业务逻辑。
     * @param file file参数，详见调用方上下文。
     * @param uploadUser uploadUser参数，详见调用方上下文。
     * @return 返回Image类型结果。
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequireLogin
    public Image upload(@RequestPart("file") MultipartFile file,
                        @RequestParam(value = "uploadUser", required = false) String uploadUser) {
        UserSession session = AuthContextHolder.requireUser();
        Image metadata = new Image();
        metadata.setUploadUser(StringUtils.hasText(uploadUser)
                ? uploadUser
                : (StringUtils.hasText(session.getDisplayName()) ? session.getDisplayName() : session.getUsername()));
        metadata.setUploaderId(session.getUserId());
        metadata.setUploaderUsername(session.getUsername());
        metadata.setUploaderDisplayName(session.getDisplayName());
        return imageService.uploadAndSave(file, metadata);
    }

    /**
     * update方法用于处理update相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @param request request参数，详见调用方上下文。
     * @return 返回Image类型结果。
     */
    @PutMapping("/{id}")
    public Image update(@PathVariable Long id, @RequestBody ImageUpdateRequest request) {
        Image image = imageService.findById(id);
        if (image == null) {
            return null;
        }
        image.setUploadUser(request.getUploadUser());
        if (request.getUploaderDisplayName() != null) {
            image.setUploaderDisplayName(request.getUploaderDisplayName());
        }
        return imageService.update(image);
    }

    /**
     * delete方法用于处理delete相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 无返回值。
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        imageService.delete(id);
    }

    /**
     * ImageUpdateRequest类用于封装ImageUpdateRequest相关的领域职责（所在包：com.busgallery.busgallery.controller）。
     */
    @Data
    public static class ImageUpdateRequest {
        private String uploadUser;
        private String uploaderDisplayName;
    }
}
