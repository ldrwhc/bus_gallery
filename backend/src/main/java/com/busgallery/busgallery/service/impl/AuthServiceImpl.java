package com.busgallery.busgallery.service.impl;

import com.busgallery.busgallery.auth.AuthContextHolder;
import com.busgallery.busgallery.auth.AuthPrincipal;
import com.busgallery.busgallery.auth.UserRole;
import com.busgallery.busgallery.auth.UserSessionService;
import com.busgallery.busgallery.dto.request.LoginRequest;
import com.busgallery.busgallery.dto.request.RegisterRequest;
import com.busgallery.busgallery.dto.response.LoginResponse;
import com.busgallery.busgallery.dto.response.UserProfileResponse;
import com.busgallery.busgallery.entity.User;
import com.busgallery.busgallery.exception.BizException;
import com.busgallery.busgallery.exception.ErrorCode;
import com.busgallery.busgallery.service.AuthSecurityService;
import com.busgallery.busgallery.service.AuthService;
import com.busgallery.busgallery.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final UserSessionService userSessionService;
    private final AuthSecurityService authSecurityService;

    @Override
    public LoginResponse register(RegisterRequest request) {
        String username = normalizeUsername(request.getUsername());
        String email = normalizeEmail(request.getEmail());
        if (userService.existsByUsername(username)) {
            throw new BizException(ErrorCode.INVALID_PARAM, "用户名已存在");
        }
        if (userService.existsByEmail(email)) {
            throw new BizException(ErrorCode.INVALID_PARAM, "邮箱已被使用");
        }
        if (!StringUtils.hasText(request.getPassword()) || !request.getPassword().equals(request.getConfirmPassword())) {
            throw new BizException(ErrorCode.INVALID_PARAM, "两次输入的密码不一致");
        }

        authSecurityService.verifyRegisterCode(request.getChallengeId(), email, request.getEmailCode());

        String displayName = StringUtils.hasText(request.getDisplayName())
                ? request.getDisplayName().trim()
                : username;
        User user = new User();
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setEmail(email);
        user.setEmailVerifiedAt(LocalDateTime.now());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setRole(userService.countUsers() == 0 ? UserRole.STATION : UserRole.USER);
        User saved = userService.save(user);

        String token = userSessionService.createSession(saved);
        UserProfileResponse profile = userService.buildProfile(saved, 0L);
        return new LoginResponse(token, profile);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        String username = normalizeUsername(request.getUsername());
        User user = userService.findByUsername(username);
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.INVALID_PARAM, "用户名或密码错误");
        }
        if (user.getRole() == null) {
            user = userService.updateRole(user.getId(), UserRole.USER, null);
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
    public AuthPrincipal currentSession() {
        return AuthContextHolder.getPrincipal();
    }

    private String normalizeUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return "";
        }
        return username.trim().toLowerCase();
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return "";
        }
        return email.trim().toLowerCase();
    }
}
