package com.busgallery.busgallery.controller;

import com.busgallery.busgallery.auth.AuthContextHolder;
import com.busgallery.busgallery.auth.AuthPrincipal;
import com.busgallery.busgallery.auth.RequireLogin;
import com.busgallery.busgallery.auth.UserSessionService;
import com.busgallery.busgallery.dto.request.UserDisplayNameUpdateRequest;
import com.busgallery.busgallery.dto.response.ImageResponse;
import com.busgallery.busgallery.dto.response.PageResponse;
import com.busgallery.busgallery.dto.response.UserProfileResponse;
import com.busgallery.busgallery.entity.Image;
import com.busgallery.busgallery.entity.User;
import com.busgallery.busgallery.entity.Vehicle;
import com.busgallery.busgallery.exception.BizException;
import com.busgallery.busgallery.exception.ErrorCode;
import com.busgallery.busgallery.service.ImageService;
import com.busgallery.busgallery.service.UserService;
import com.busgallery.busgallery.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * UserController类用于封装UserController相关的领域职责（所在包：com.busgallery.busgallery.controller）。
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private static final int FIXED_IMAGE_PAGE_SIZE = 12;

    private final UserService userService;
    private final UserSessionService userSessionService;
    private final ImageService imageService;
    private final VehicleService vehicleService;

    /**
     * currentUser方法用于处理currentUser相关的业务逻辑。
     * @return 返回UserProfileResponse类型结果。
     */
    @GetMapping("/me")
    @RequireLogin
    public UserProfileResponse currentUser() {
        AuthPrincipal session = AuthContextHolder.requirePrincipal();
        User user = userService.findById(session.getUserId());
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        long uploads = userService.countUserImages(user.getId());
        return userService.buildProfile(user, uploads);
    }

    @PutMapping("/me/display-name")
    @RequireLogin
    public UserProfileResponse updateDisplayName(@Valid @RequestBody UserDisplayNameUpdateRequest request) {
        AuthPrincipal session = AuthContextHolder.requirePrincipal();
        User user = userService.updateDisplayName(session.getUserId(), request.getDisplayName());
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        userSessionService.refreshDisplayNameByUserId(user.getId(), user.getDisplayName());
        long uploads = userService.countUserImages(user.getId());
        return userService.buildProfile(user, uploads);
    }

    /**
     * myImages方法用于处理myImages相关的业务逻辑。
     * @param page page参数，详见调用方上下文。
     * @param size size参数，详见调用方上下文。
     * @return 返回PageResponse<ImageResponse>类型结果。
     */
    @GetMapping("/me/images")
    @RequireLogin
    public PageResponse<ImageResponse> myImages(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        AuthPrincipal session = AuthContextHolder.requirePrincipal();
        User user = userService.findById(session.getUserId());
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        List<Image> images = imageService.listByUploader(user.getId(), page, FIXED_IMAGE_PAGE_SIZE);
        List<ImageResponse> records = images.stream()
                .map(img -> {
                    Vehicle v = img.getVehicleId() != null ? vehicleService.findById(img.getVehicleId()) : null;
                    return ImageResponse.fromEntity(img, v);
                })
                .collect(Collectors.toList());
        long total = userService.countUserImages(user.getId());
        int pageNo = Math.max(page, 1);
        int pageSize = FIXED_IMAGE_PAGE_SIZE;
        return PageResponse.of(records, total, pageNo, pageSize);
    }

    /**
     * getUserProfile方法用于处理getUserProfile相关的业务逻辑。
     * @param userId userId参数，详见调用方上下文。
     * @return 返回UserProfileResponse类型结果。
     */
    @GetMapping("/{userId}")
    public UserProfileResponse getUserProfile(@PathVariable Long userId) {
        User user = userService.findById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        long uploads = userService.countUserImages(userId);
        return userService.buildProfile(user, uploads);
    }

    /**
     * getUserImages方法用于处理getUserImages相关的业务逻辑。
     * @param userId userId参数，详见调用方上下文。
     * @param page page参数，详见调用方上下文。
     * @param size size参数，详见调用方上下文。
     * @return 返回PageResponse<ImageResponse>类型结果。
     */
    @GetMapping("/{userId}/images")
    public PageResponse<ImageResponse> getUserImages(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        User user = userService.findById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        List<Image> images = imageService.listByUploader(userId, page, FIXED_IMAGE_PAGE_SIZE);
        List<ImageResponse> records = images.stream()
                .map(img -> {
                    Vehicle v = img.getVehicleId() != null ? vehicleService.findById(img.getVehicleId()) : null;
                    return ImageResponse.fromEntity(img, v);
                })
                .collect(Collectors.toList());
        long total = userService.countUserImages(userId);
        int pageNo = Math.max(page, 1);
        int pageSize = FIXED_IMAGE_PAGE_SIZE;
        return PageResponse.of(records, total, pageNo, pageSize);
    }
}
