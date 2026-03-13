package com.busgallery.busgallery.controller;

import com.busgallery.busgallery.auth.AuthContextHolder;
import com.busgallery.busgallery.auth.RequireLogin;
import com.busgallery.busgallery.auth.UserSession;
import com.busgallery.busgallery.dto.response.ImageResponse;
import com.busgallery.busgallery.dto.response.PageResponse;
import com.busgallery.busgallery.dto.response.UserProfileResponse;
import com.busgallery.busgallery.entity.Image;
import com.busgallery.busgallery.entity.User;
import com.busgallery.busgallery.exception.BizException;
import com.busgallery.busgallery.exception.ErrorCode;
import com.busgallery.busgallery.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @RequireLogin
    public UserProfileResponse currentUser() {
        UserSession session = AuthContextHolder.requireUser();
        User user = userService.findById(session.getUserId());
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        long uploads = userService.countUserImages(user.getId());
        return userService.buildProfile(user, uploads);
    }

    @GetMapping("/me/images")
    @RequireLogin
    public PageResponse<ImageResponse> myImages(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        UserSession session = AuthContextHolder.requireUser();
        User user = userService.findById(session.getUserId());
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        List<Image> images = userService.listUserImages(user.getId(), page, size);
        List<ImageResponse> records = images.stream()
                .map(ImageResponse::fromEntity)
                .collect(Collectors.toList());
        long total = userService.countUserImages(user.getId());
        int pageNo = Math.max(page, 1);
        int pageSize = Math.max(size, 1);
        return PageResponse.of(records, total, pageNo, pageSize);
    }

    @GetMapping("/{userId}")
    public UserProfileResponse getUserProfile(@PathVariable Long userId) {
        User user = userService.findById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        long uploads = userService.countUserImages(userId);
        return userService.buildProfile(user, uploads);
    }

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
        List<Image> images = userService.listUserImages(userId, page, size);
        List<ImageResponse> records = images.stream()
                .map(ImageResponse::fromEntity)
                .collect(Collectors.toList());
        long total = userService.countUserImages(userId);
        int pageNo = Math.max(page, 1);
        int pageSize = Math.max(size, 1);
        return PageResponse.of(records, total, pageNo, pageSize);
    }
}
