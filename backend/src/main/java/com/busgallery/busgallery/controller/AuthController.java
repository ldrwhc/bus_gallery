package com.busgallery.busgallery.controller;

import com.busgallery.busgallery.auth.AuthContextHolder;
import com.busgallery.busgallery.auth.RequireLogin;
import com.busgallery.busgallery.auth.UserSession;
import com.busgallery.busgallery.dto.request.*;
import com.busgallery.busgallery.dto.response.AuthChallengeResponse;
import com.busgallery.busgallery.dto.response.AuthTicketResponse;
import com.busgallery.busgallery.dto.response.LoginResponse;
import com.busgallery.busgallery.dto.response.SimpleSuccessResponse;
import com.busgallery.busgallery.service.AuthSecurityService;
import com.busgallery.busgallery.service.AuthService;
import com.busgallery.busgallery.util.RequestIpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthSecurityService authSecurityService;

    @PostMapping("/register/send-email-code")
    public AuthChallengeResponse sendRegisterEmailCode(@Valid @RequestBody RegisterSendCodeRequest request,
                                                       HttpServletRequest httpRequest) {
        String ip = RequestIpUtil.resolveClientIp(httpRequest);
        return authSecurityService.sendRegisterCode(request.getUsername(), request.getEmail(), ip);
    }

    @PostMapping("/register")
    public LoginResponse register(@Valid @RequestBody RegisterRequest request,
                                  HttpServletRequest httpRequest) {
        String ip = RequestIpUtil.resolveClientIp(httpRequest);
        authSecurityService.checkRegisterSubmitRateLimit(ip, request.getEmail());
        return authService.register(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request,
                               HttpServletRequest httpRequest) {
        String ip = RequestIpUtil.resolveClientIp(httpRequest);
        authSecurityService.checkLoginRateLimit(ip, request.getUsername());
        return authService.login(request);
    }

    @PostMapping("/logout")
    @RequireLogin
    public void logout() {
        if (AuthContextHolder.get() != null) {
            authService.logout(AuthContextHolder.get().getToken());
        }
    }

    @PostMapping("/password/change/send-email-code")
    @RequireLogin
    public AuthChallengeResponse sendChangePasswordCode(@Valid @RequestBody PasswordChangeSendCodeRequest request,
                                                        HttpServletRequest httpRequest) {
        UserSession session = AuthContextHolder.requireUser();
        String ip = RequestIpUtil.resolveClientIp(httpRequest);
        return authSecurityService.sendPasswordChangeCode(session, request.getCurrentPassword(), ip);
    }

    @PostMapping("/password/change/confirm")
    @RequireLogin
    public SimpleSuccessResponse confirmChangePassword(@Valid @RequestBody PasswordChangeConfirmRequest request) {
        UserSession session = AuthContextHolder.requireUser();
        authSecurityService.changePassword(
                session,
                request.getChallengeId(),
                request.getEmailCode(),
                request.getNewPassword(),
                request.getConfirmPassword()
        );
        return SimpleSuccessResponse.ok("密码修改成功，请重新登录");
    }

    @PostMapping("/password/forgot/send-email-code")
    public AuthChallengeResponse sendForgotPasswordCode(@Valid @RequestBody ForgotPasswordSendCodeRequest request,
                                                        HttpServletRequest httpRequest) {
        String ip = RequestIpUtil.resolveClientIp(httpRequest);
        return authSecurityService.sendForgotPasswordCode(request.getUsernameOrEmail(), ip);
    }

    @PostMapping("/password/forgot/verify-email-code")
    public AuthTicketResponse verifyForgotPasswordCode(@Valid @RequestBody ForgotPasswordVerifyCodeRequest request) {
        return authSecurityService.verifyForgotPasswordCode(request.getChallengeId(), request.getEmailCode());
    }

    @PostMapping("/password/forgot/reset")
    public SimpleSuccessResponse resetForgotPassword(@Valid @RequestBody ForgotPasswordResetRequest request) {
        authSecurityService.resetForgotPassword(request.getResetTicket(), request.getNewPassword(), request.getConfirmPassword());
        return SimpleSuccessResponse.ok("密码重置成功，请登录");
    }

    @PostMapping("/email/bind/send-email-code")
    @RequireLogin
    public AuthChallengeResponse sendBindEmailCode(@Valid @RequestBody EmailBindSendCodeRequest request,
                                                   HttpServletRequest httpRequest) {
        UserSession session = AuthContextHolder.requireUser();
        String ip = RequestIpUtil.resolveClientIp(httpRequest);
        return authSecurityService.sendBindEmailCode(session, request.getEmail(), request.getCurrentPassword(), ip);
    }

    @PostMapping("/email/bind/confirm")
    @RequireLogin
    public SimpleSuccessResponse confirmBindEmail(@Valid @RequestBody EmailBindConfirmRequest request) {
        UserSession session = AuthContextHolder.requireUser();
        authSecurityService.confirmBindEmail(session, request.getChallengeId(), request.getEmail(), request.getEmailCode());
        return SimpleSuccessResponse.ok("邮箱绑定成功");
    }
}

