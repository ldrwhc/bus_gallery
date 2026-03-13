package com.busgallery.busgallery.service.impl;

import com.busgallery.busgallery.auth.AuthContextHolder;
import com.busgallery.busgallery.auth.UserSession;
import com.busgallery.busgallery.auth.UserSessionService;
import com.busgallery.busgallery.dto.request.LoginRequest;
import com.busgallery.busgallery.dto.request.RegisterRequest;
import com.busgallery.busgallery.dto.response.LoginResponse;
import com.busgallery.busgallery.dto.response.UserProfileResponse;
import com.busgallery.busgallery.entity.User;
import com.busgallery.busgallery.exception.BizException;
import com.busgallery.busgallery.exception.ErrorCode;
import com.busgallery.busgallery.service.AuthService;
import com.busgallery.busgallery.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final UserSessionService userSessionService;

    @Override
    public LoginResponse register(RegisterRequest request) {
        String username = request.getUsername().trim().toLowerCase();
        if (userService.existsByUsername(username)) {
            throw new BizException(ErrorCode.INVALID_PARAM, "用户名已存在");
        }
        String displayName = StringUtils.hasText(request.getDisplayName())
                ? request.getDisplayName().trim()
                : username;
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BizException(ErrorCode.INVALID_PARAM, "两次输入的密码不一致");
        }
        User user = new User();
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        User saved = userService.save(user);
        String token = userSessionService.createSession(saved);
        UserProfileResponse profile = userService.buildProfile(saved, 0L);
        return new LoginResponse(token, profile);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername().trim().toLowerCase();
        User user = userService.findByUsername(username);
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.INVALID_PARAM, "用户名或密码错误");
        }
        String token = userSessionService.createSession(user);
        long uploads = userService.countUserImages(user.getId());
        UserProfileResponse profile = userService.buildProfile(user, uploads);
        return new LoginResponse(token, profile);
    }

    @Override
    public void logout(String token) {
        userSessionService.deleteSession(token);
    }

    @Override
    public UserSession currentSession() {
        return AuthContextHolder.get();
    }
}
