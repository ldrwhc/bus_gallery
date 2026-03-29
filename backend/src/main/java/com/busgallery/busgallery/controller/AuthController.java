package com.busgallery.busgallery.controller;

import com.busgallery.busgallery.auth.AuthContextHolder;
import com.busgallery.busgallery.auth.AuthPrincipal;
import com.busgallery.busgallery.auth.RequireLogin;
import com.busgallery.busgallery.config.AuthSecurityProperties;
import com.busgallery.busgallery.dto.request.EmailBindConfirmRequest;
import com.busgallery.busgallery.dto.request.EmailBindSendCodeRequest;
import com.busgallery.busgallery.dto.request.ForgotPasswordResetRequest;
import com.busgallery.busgallery.dto.request.ForgotPasswordSendCodeRequest;
import com.busgallery.busgallery.dto.request.ForgotPasswordVerifyCodeRequest;
import com.busgallery.busgallery.dto.request.LoginRequest;
import com.busgallery.busgallery.dto.request.PasswordChangeConfirmRequest;
import com.busgallery.busgallery.dto.request.PasswordChangeSendCodeRequest;
import com.busgallery.busgallery.dto.request.PasswordVerifyRequest;
import com.busgallery.busgallery.dto.request.RegisterRequest;
import com.busgallery.busgallery.dto.request.RegisterSendCodeRequest;
import com.busgallery.busgallery.dto.response.AuthChallengeResponse;
import com.busgallery.busgallery.dto.response.AuthTicketResponse;
import com.busgallery.busgallery.dto.response.CaptchaChallengeResponse;
import com.busgallery.busgallery.dto.response.LoginResponse;
import com.busgallery.busgallery.dto.response.SimpleSuccessResponse;
import com.busgallery.busgallery.exception.BizException;
import com.busgallery.busgallery.exception.ErrorCode;
import com.busgallery.busgallery.service.AuthSecurityService;
import com.busgallery.busgallery.service.AuthService;
import com.busgallery.busgallery.service.HumanVerificationService;
import com.busgallery.busgallery.service.RateLimitService;
import com.busgallery.busgallery.util.RequestIpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthSecurityService authSecurityService;
    private final HumanVerificationService humanVerificationService;
    private final RateLimitService rateLimitService;
    private final AuthSecurityProperties authSecurityProperties;

    @GetMapping("/captcha")
    public CaptchaChallengeResponse issueCaptcha(@RequestParam(defaultValue = "login") String scene,
                                                 HttpServletRequest httpRequest) {
        String ip = RequestIpUtil.resolveClientIp(httpRequest);
        return humanVerificationService.issueCaptcha(scene, ip);
    }

    @PostMapping("/register/send-email-code")
    public AuthChallengeResponse sendRegisterEmailCode(@Valid @RequestBody RegisterSendCodeRequest request,
                                                       HttpServletRequest httpRequest) {
        String ip = RequestIpUtil.resolveClientIp(httpRequest);
        applySendCodeDailyLimit("register-send", ip, request.getEmail(), request.getUsername());
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
        humanVerificationService.assertLoginCaptchaIfTriggered(ip, request.getUsername(), request.getCaptchaId(), request.getCaptchaCode());
        authSecurityService.checkLoginRateLimit(ip, request.getUsername());
        try {
            LoginResponse response = authService.login(request);
            humanVerificationService.onLoginSuccess(ip, request.getUsername());
            return response;
        } catch (BizException ex) {
            if (ex.getErrorCode() == ErrorCode.INVALID_PARAM || ex.getErrorCode() == ErrorCode.UNAUTHORIZED) {
                humanVerificationService.onLoginFailed(ip, request.getUsername());
            }
            throw ex;
        }
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
        AuthPrincipal session = AuthContextHolder.requirePrincipal();
        String ip = RequestIpUtil.resolveClientIp(httpRequest);
        applySendCodeDailyLimit("change-password-send", ip, null, session.getUsername());
        return authSecurityService.sendPasswordChangeCode(session, request.getCurrentPassword(), ip);
    }

    @PostMapping("/password/verify")
    @RequireLogin
    public SimpleSuccessResponse verifyPassword(@Valid @RequestBody PasswordVerifyRequest request) {
        AuthPrincipal session = AuthContextHolder.requirePrincipal();
        authSecurityService.verifyCurrentPassword(session, request.getCurrentPassword());
        return SimpleSuccessResponse.ok("password verified");
    }

    @PostMapping("/password/change/confirm")
    @RequireLogin
    public SimpleSuccessResponse confirmChangePassword(@Valid @RequestBody PasswordChangeConfirmRequest request) {
        AuthPrincipal session = AuthContextHolder.requirePrincipal();
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
        humanVerificationService.assertForgotCaptchaIfTriggered(ip, request.getUsernameOrEmail(), request.getCaptchaId(), request.getCaptchaCode());
        applySendCodeDailyLimit("forgot-send", ip, null, request.getUsernameOrEmail());
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
        AuthPrincipal session = AuthContextHolder.requirePrincipal();
        String ip = RequestIpUtil.resolveClientIp(httpRequest);
        applySendCodeDailyLimit("bind-email-send", ip, request.getEmail(), session.getUsername());
        return authSecurityService.sendBindEmailCode(session, request.getEmail(), request.getCurrentPassword(), ip);
    }

    @PostMapping("/email/bind/confirm")
    @RequireLogin
    public SimpleSuccessResponse confirmBindEmail(@Valid @RequestBody EmailBindConfirmRequest request) {
        AuthPrincipal session = AuthContextHolder.requirePrincipal();
        authSecurityService.confirmBindEmail(session, request.getChallengeId(), request.getEmail(), request.getEmailCode());
        return SimpleSuccessResponse.ok("邮箱绑定成功");
    }

    private void applySendCodeDailyLimit(String scope, String clientIp, String email, String account) {
        AuthSecurityProperties.RateLimit rate = authSecurityProperties.getRateLimit();
        String ip = StringUtils.hasText(clientIp) ? clientIp.trim() : "unknown";
        rateLimitService.check(scope, "ip-day", ip, rate.getSendCodeIpPerDay(), Duration.ofDays(1), "当前 IP 今日验证码请求次数过多，请明日再试");
        String normalizedEmail = normalizeText(email);
        if (StringUtils.hasText(normalizedEmail)) {
            rateLimitService.check(scope, "email-day", normalizedEmail, rate.getSendCodeEmailPerDay(), Duration.ofDays(1), "该邮箱今日验证码次数过多，请明日再试");
        }
        String normalizedAccount = normalizeText(account);
        if (StringUtils.hasText(normalizedAccount)) {
            rateLimitService.check(scope, "account-day", normalizedAccount, rate.getSendCodeAccountPerDay(), Duration.ofDays(1), "该账号今日验证码请求次数过多，请明日再试");
        }
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().toLowerCase();
    }
}
