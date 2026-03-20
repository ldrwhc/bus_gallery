package com.busgallery.busgallery.controller;

import com.busgallery.busgallery.auth.AuthContextHolder;
import com.busgallery.busgallery.auth.RequireLogin;
import com.busgallery.busgallery.auth.UserRole;
import com.busgallery.busgallery.auth.UserSession;
import com.busgallery.busgallery.entity.Image;
import com.busgallery.busgallery.entity.Vehicle;
import com.busgallery.busgallery.exception.BizException;
import com.busgallery.busgallery.exception.ErrorCode;
import com.busgallery.busgallery.mapper.VehicleImageMapper;
import com.busgallery.busgallery.service.ImageAccessService;
import com.busgallery.busgallery.service.ImageService;
import com.busgallery.busgallery.service.RegionService;
import com.busgallery.busgallery.service.UploadSecurityService;
import com.busgallery.busgallery.service.VehicleService;
import com.busgallery.busgallery.util.RequestIpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private static final int FIXED_LATEST_LIMIT = 12;

    private final ImageAccessService imageAccessService;
    private final ImageService imageService;
    private final UploadSecurityService uploadSecurityService;
    private final VehicleImageMapper vehicleImageMapper;
    private final VehicleService vehicleService;
    private final RegionService regionService;

    @GetMapping("/{id}")
    public Image detail(@PathVariable Long id) {
        return imageService.findById(id);
    }

    @GetMapping("/latest")
    public List<Image> latest(@RequestParam(defaultValue = "12") int limit) {
        return imageService.listLatest(FIXED_LATEST_LIMIT);
    }

    @GetMapping("/vehicle/{vehicleId}")
    public List<Image> listByVehicle(@PathVariable Long vehicleId) {
        return imageService.listByVehicle(vehicleId);
    }

    @GetMapping("/access/{token}")
    public ResponseEntity<InputStreamResource> access(@PathVariable String token) {
        ImageAccessService.SignedImageStream signed = imageAccessService.resolveStream(token);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=60")
                .header("X-Robots-Tag", "noindex, noimageindex")
                .contentLength(Math.max(0, signed.getContentLength()))
                .contentType(MediaType.parseMediaType(signed.getContentType()))
                .body(new InputStreamResource(signed.getInputStream()));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequireLogin
    public Image upload(@RequestPart("file") MultipartFile file,
                        @RequestParam(value = "uploadUser", required = false) String uploadUser,
                        HttpServletRequest httpRequest) {
        UserSession session = AuthContextHolder.requireUser();
        uploadSecurityService.checkUploadQuotaAndRate(session, RequestIpUtil.resolveClientIp(httpRequest));

        Image metadata = new Image();
        metadata.setUploadUser(StringUtils.hasText(uploadUser)
                ? uploadUser
                : (StringUtils.hasText(session.getDisplayName()) ? session.getDisplayName() : session.getUsername()));
        metadata.setUploaderId(session.getUserId());
        metadata.setUploaderUsername(session.getUsername());
        metadata.setUploaderDisplayName(session.getDisplayName());
        return imageService.uploadAndSave(file, metadata);
    }

    @PutMapping("/{id}")
    @RequireLogin
    public Image update(@PathVariable Long id, @RequestBody ImageUpdateRequest request) {
        Image image = imageService.findRawById(id);
        if (image == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "图片不存在");
        }
        UserSession session = AuthContextHolder.requireUser();
        assertImageWritePermission(session, image);

        image.setUploadUser(request.getUploadUser());
        if (request.getUploaderDisplayName() != null) {
            image.setUploaderDisplayName(request.getUploaderDisplayName());
        }
        return imageService.update(image);
    }

    @DeleteMapping("/{id}")
    @RequireLogin
    public void delete(@PathVariable Long id) {
        Image image = imageService.findRawById(id);
        if (image == null) {
            return;
        }
        UserSession session = AuthContextHolder.requireUser();
        assertImageWritePermission(session, image);
        imageService.delete(id);
    }

    private void assertImageWritePermission(UserSession session, Image image) {
        if (session.getRole() == UserRole.STATION) {
            return;
        }
        if (session.getRole() == UserRole.USER) {
            if (!session.getUserId().equals(image.getUploaderId())) {
                throw new BizException(ErrorCode.UNAUTHORIZED, "只能修改自己上传的图片");
            }
            return;
        }
        if (session.getRole() == UserRole.REVIEWER) {
            if (session.getUserId().equals(image.getUploaderId())) {
                return;
            }
            Long vehicleId = vehicleImageMapper.selectPrimaryVehicleIdByImageId(image.getId());
            if (vehicleId == null) {
                throw new BizException(ErrorCode.UNAUTHORIZED, "无权修改该图片");
            }
            Vehicle vehicle = vehicleService.findById(vehicleId);
            Long regionId = vehicle != null && vehicle.getRegion() != null ? vehicle.getRegion().getId() : null;
            if (!isRegionInReviewerScope(session, regionId)) {
                throw new BizException(ErrorCode.UNAUTHORIZED, "该图片不在你的审核地区");
            }
            return;
        }
        throw new BizException(ErrorCode.UNAUTHORIZED, "无权修改该图片");
    }

    private boolean isRegionInReviewerScope(UserSession session, Long targetRegionId) {
        if (targetRegionId == null || session.getReviewRegionId() == null) {
            return false;
        }
        Long reviewerProvinceId = regionService.resolveProvinceId(session.getReviewRegionId());
        Long targetProvinceId = regionService.resolveProvinceId(targetRegionId);
        return reviewerProvinceId != null && reviewerProvinceId.equals(targetProvinceId);
    }

    @Data
    public static class ImageUpdateRequest {
        private String uploadUser;
        private String uploaderDisplayName;
    }
}
