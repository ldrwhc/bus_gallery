package com.busgallery.busgallery.service.impl;

import com.busgallery.busgallery.auth.AuthPrincipal;
import com.busgallery.busgallery.auth.UserSessionService;
import com.busgallery.busgallery.config.AuthSecurityProperties;
import com.busgallery.busgallery.dto.response.AuthChallengeResponse;
import com.busgallery.busgallery.dto.response.AuthTicketResponse;
import com.busgallery.busgallery.entity.User;
import com.busgallery.busgallery.exception.BizException;
import com.busgallery.busgallery.exception.ErrorCode;
import com.busgallery.busgallery.service.AuthSecurityService;
import com.busgallery.busgallery.service.MailService;
import com.busgallery.busgallery.service.RateLimitService;
import com.busgallery.busgallery.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthSecurityServiceImpl implements AuthSecurityService {

    private static final String OTP_KEY_PREFIX = "busgallery:auth:otp:";
    private static final String OTP_COOLDOWN_KEY_PREFIX = "busgallery:auth:otp:cooldown:";
    private static final String RESET_TICKET_KEY_PREFIX = "busgallery:auth:ticket:reset:";

    private final UserService userService;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final AuthSecurityProperties properties;
    private final RateLimitService rateLimitService;
    private final UserSessionService userSessionService;

    @Qualifier("authMailExecutor")
    private final Executor authMailExecutor;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public void checkLoginRateLimit(String clientIp, String username) {
        AuthSecurityProperties.RateLimit rate = properties.getRateLimit();
        String ip = normalizeClientIp(clientIp);
        String userKey = normalizeIdentifier(username);
        rateLimitService.check("auth", "global", "all", rate.getAuthGlobalPerMinute(), Duration.ofMinutes(1), "请求过于频繁，请稍后重试");
        rateLimitService.check("login", "ip", ip, rate.getLoginIpPerMinute(), Duration.ofMinutes(1), "登录过于频繁，请稍后重试");
        if (StringUtils.hasText(userKey)) {
            rateLimitService.check("login", "user", userKey, rate.getLoginUserPerMinute(), Duration.ofMinutes(1), "账号尝试次数过多，请稍后再试");
        }
    }

    @Override
    public void checkRegisterSubmitRateLimit(String clientIp, String email) {
        AuthSecurityProperties.RateLimit rate = properties.getRateLimit();
        String ip = normalizeClientIp(clientIp);
        String normalizedEmail = normalizeEmail(email);
        rateLimitService.check("auth", "global", "all", rate.getAuthGlobalPerMinute(), Duration.ofMinutes(1), "请求过于频繁，请稍后重试");
        rateLimitService.check("register", "ip", ip, rate.getRegisterIpPerMinute(), Duration.ofMinutes(1), "注册操作过于频繁，请稍后再试");
        if (StringUtils.hasText(normalizedEmail)) {
            rateLimitService.check("register", "email", normalizedEmail, rate.getRegisterEmailPerHour(), Duration.ofHours(1), "该邮箱注册操作过于频繁，请稍后再试");
        }
    }

    @Override
    public AuthChallengeResponse sendRegisterCode(String username, String email, String clientIp) {
        String normalizedUsername = normalizeUsername(username);
        String normalizedEmail = normalizeEmail(email);
        if (!StringUtils.hasText(normalizedUsername) || !StringUtils.hasText(normalizedEmail)) {
            throw new BizException(ErrorCode.INVALID_PARAM, "用户名和邮箱不能为空");
        }
        if (userService.existsByUsername(normalizedUsername)) {
            throw new BizException(ErrorCode.INVALID_PARAM, "用户名已存在");
        }
        if (userService.existsByEmail(normalizedEmail)) {
            throw new BizException(ErrorCode.INVALID_PARAM, "邮箱已被使用");
        }
        checkSendCodeRate("register", clientIp, normalizedEmail);
        return issueChallenge(ChallengeScene.REGISTER, null, normalizedEmail, normalizedUsername, false, "注册验证");
    }

    @Override
    public void verifyRegisterCode(String challengeId, String email, String emailCode) {
        String normalizedEmail = normalizeEmail(email);
        OtpChallenge challenge = verifyChallenge(ChallengeScene.REGISTER, challengeId, emailCode);
        if (!normalizedEmail.equalsIgnoreCase(challenge.getEmail())) {
            throw new BizException(ErrorCode.INVALID_PARAM, "验证码与邮箱不匹配");
        }
    }

    @Override
    public AuthChallengeResponse sendPasswordChangeCode(AuthPrincipal session, String currentPassword, String clientIp) {
        if (session == null || session.getUserId() == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        User user = userService.findById(session.getUserId());
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BizException(ErrorCode.INVALID_PARAM, "当前密码错误");
        }
        String email = normalizeEmail(user.getEmail());
        if (!StringUtils.hasText(email) || user.getEmailVerifiedAt() == null) {
            throw new BizException(ErrorCode.INVALID_PARAM, "请先绑定并验证邮箱");
        }
        checkSendCodeRate("change-password", clientIp, email);
        return issueChallenge(ChallengeScene.CHANGE_PASSWORD, user.getId(), email, user.getUsername(), false, "修改密码");
    }

    @Override
    @Transactional
    public void changePassword(AuthPrincipal session, String challengeId, String emailCode, String newPassword, String confirmPassword) {
        if (session == null || session.getUserId() == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        validateNewPassword(newPassword, confirmPassword);
        OtpChallenge challenge = verifyChallenge(ChallengeScene.CHANGE_PASSWORD, challengeId, emailCode);
        if (!session.getUserId().equals(challenge.getUserId())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "验证码不属于当前用户");
        }
        User user = userService.findById(session.getUserId());
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        userService.updatePassword(user.getId(), passwordEncoder.encode(newPassword), LocalDateTime.now());
        userSessionService.deleteAllSessionsByUserId(user.getId());
    }

    @Override
    public AuthChallengeResponse sendForgotPasswordCode(String usernameOrEmail, String clientIp) {
        String identifier = normalizeIdentifier(usernameOrEmail);
        if (!StringUtils.hasText(identifier)) {
            throw new BizException(ErrorCode.INVALID_PARAM, "请输入用户名或邮箱");
        }
        AuthSecurityProperties.RateLimit rate = properties.getRateLimit();
        String ip = normalizeClientIp(clientIp);
        rateLimitService.check("auth", "global", "all", rate.getAuthGlobalPerMinute(), Duration.ofMinutes(1), "请求过于频繁，请稍后重试");
        rateLimitService.check("forgot", "ip", ip, rate.getForgotIpPerMinute(), Duration.ofMinutes(1), "找回密码请求过于频繁，请稍后再试");
        rateLimitService.check("forgot", "identifier", identifier, rate.getForgotIdentifierPerHour(), Duration.ofHours(1), "该账号找回次数过多，请稍后再试");

        User user = resolveUserByIdentifier(identifier);
        if (user == null || !StringUtils.hasText(user.getEmail()) || user.getEmailVerifiedAt() == null) {
            return issueChallenge(ChallengeScene.FORGOT_PASSWORD, null, "dummy@example.invalid", identifier, true, "找回密码");
        }
        String email = normalizeEmail(user.getEmail());
        checkSendCodeRate("forgot-password", clientIp, email);
        return issueChallenge(ChallengeScene.FORGOT_PASSWORD, user.getId(), email, identifier, false, "找回密码");
    }

    @Override
    public AuthTicketResponse verifyForgotPasswordCode(String challengeId, String emailCode) {
        OtpChallenge challenge = verifyChallenge(ChallengeScene.FORGOT_PASSWORD, challengeId, emailCode);
        if (challenge.isDummy() || challenge.getUserId() == null) {
            throw new BizException(ErrorCode.INVALID_PARAM, "验证码错误或已过期");
        }
        String ticket = UUID.randomUUID().toString().replace("-", "");
        ResetTicketPayload payload = new ResetTicketPayload(challenge.getUserId(), challenge.getEmail());
        saveResetTicket(ticket, payload);
        return AuthTicketResponse.builder()
                .ticket(ticket)
                .expireInSeconds(properties.getResetTicketTtlSeconds())
                .build();
    }

    @Override
    @Transactional
    public void resetForgotPassword(String resetTicket, String newPassword, String confirmPassword) {
        validateNewPassword(newPassword, confirmPassword);
        ResetTicketPayload payload = consumeResetTicket(resetTicket);
        User user = payload == null ? null : userService.findById(payload.getUserId());
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        userService.updatePassword(user.getId(), passwordEncoder.encode(newPassword), LocalDateTime.now());
        userSessionService.deleteAllSessionsByUserId(user.getId());
    }

    @Override
    public AuthChallengeResponse sendBindEmailCode(AuthPrincipal session, String email, String currentPassword, String clientIp) {
        if (session == null || session.getUserId() == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        User user = userService.findById(session.getUserId());
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BizException(ErrorCode.INVALID_PARAM, "当前密码错误");
        }
        String normalizedEmail = normalizeEmail(email);
        User existing = userService.findByEmail(normalizedEmail);
        if (existing != null && !existing.getId().equals(user.getId())) {
            throw new BizException(ErrorCode.INVALID_PARAM, "邮箱已被其他用户占用");
        }
        checkSendCodeRate("bind-email", clientIp, normalizedEmail);
        return issueChallenge(ChallengeScene.BIND_EMAIL, user.getId(), normalizedEmail, user.getUsername(), false, "绑定邮箱");
    }

    @Override
    @Transactional
    public void confirmBindEmail(AuthPrincipal session, String challengeId, String email, String emailCode) {
        if (session == null || session.getUserId() == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        OtpChallenge challenge = verifyChallenge(ChallengeScene.BIND_EMAIL, challengeId, emailCode);
        String normalizedEmail = normalizeEmail(email);
        if (!session.getUserId().equals(challenge.getUserId()) || !normalizedEmail.equalsIgnoreCase(challenge.getEmail())) {
            throw new BizException(ErrorCode.INVALID_PARAM, "验证码与目标邮箱不匹配");
        }
        User existing = userService.findByEmail(normalizedEmail);
        if (existing != null && !existing.getId().equals(session.getUserId())) {
            throw new BizException(ErrorCode.INVALID_PARAM, "邮箱已被其他用户占用");
        }
        userService.bindEmail(session.getUserId(), normalizedEmail, LocalDateTime.now());
    }

    private void checkSendCodeRate(String scope, String clientIp, String email) {
        AuthSecurityProperties.RateLimit rate = properties.getRateLimit();
        String ip = normalizeClientIp(clientIp);
        rateLimitService.check("auth", "global", "all", rate.getAuthGlobalPerMinute(), Duration.ofMinutes(1), "请求过于频繁，请稍后重试");
        rateLimitService.check(scope, "ip", ip, rate.getSendCodeIpPerMinute(), Duration.ofMinutes(1), "验证码发送过于频繁，请稍后再试");
        rateLimitService.check(scope, "email", email, rate.getSendCodeEmailPerHour(), Duration.ofHours(1), "该邮箱验证码请求过多，请稍后再试");
        applySendCooldown(scope, email);
    }

    private void applySendCooldown(String scope, String email) {
        String key = OTP_COOLDOWN_KEY_PREFIX + scope + ":" + sha256(email);
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(
                key,
                "1",
                Duration.ofSeconds(Math.max(1, properties.getOtpResendCooldownSeconds()))
        );
        if (Boolean.FALSE.equals(ok)) {
            throw new BizException(ErrorCode.REQUEST_DUPLICATE, "验证码发送过快，请稍后再试");
        }
    }

    private AuthChallengeResponse issueChallenge(ChallengeScene scene,
                                                 Long userId,
                                                 String email,
                                                 String identifier,
                                                 boolean dummy,
                                                 String sceneLabel) {
        if (!dummy) {
            ensureMailReady();
        }
        String challengeId = UUID.randomUUID().toString().replace("-", "");
        String code = generateNumericCode(properties.getCodeLength());
        String salt = UUID.randomUUID().toString().replace("-", "");
        OtpChallenge payload = OtpChallenge.builder()
                .scene(scene.name())
                .userId(userId)
                .email(email)
                .identifier(identifier)
                .dummy(dummy)
                .salt(salt)
                .codeHash(hashCode(code, salt))
                .attempts(0)
                .maxAttempts(Math.max(1, properties.getOtpMaxAttempts()))
                .createdAt(System.currentTimeMillis())
                .build();
        saveChallenge(challengeId, payload);
        if (!dummy) {
            dispatchOtpEmail(email, sceneLabel, code);
        }
        return AuthChallengeResponse.builder()
                .challengeId(challengeId)
                .expireInSeconds(properties.getOtpTtlSeconds())
                .resendAfterSeconds(properties.getOtpResendCooldownSeconds())
                .build();
    }

    private OtpChallenge verifyChallenge(ChallengeScene scene, String challengeId, String inputCode) {
        OtpChallenge challenge = getChallenge(scene, challengeId);
        if (challenge == null || !StringUtils.hasText(inputCode)) {
            throw new BizException(ErrorCode.INVALID_PARAM, "验证码错误或已过期");
        }
        boolean matched = hashCode(inputCode.trim(), challenge.getSalt()).equals(challenge.getCodeHash());
        if (!matched) {
            challenge.setAttempts(challenge.getAttempts() + 1);
            if (challenge.getAttempts() >= challenge.getMaxAttempts()) {
                deleteChallenge(scene, challengeId);
            } else {
                saveChallenge(challengeId, challenge);
            }
            throw new BizException(ErrorCode.INVALID_PARAM, "验证码错误或已过期");
        }
        deleteChallenge(scene, challengeId);
        return challenge;
    }

    private void dispatchOtpEmail(String email, String sceneLabel, String code) {
        try {
            authMailExecutor.execute(() -> {
                try {
                    mailService.sendOtpCode(email, sceneLabel, code, properties.getOtpTtlSeconds());
                } catch (Exception ex) {
                    log.error("Send OTP email failed: email={}, scene={}", email, sceneLabel, ex);
                }
            });
        } catch (RejectedExecutionException ex) {
            throw new BizException(ErrorCode.BUSINESS_ERROR, "验证码服务繁忙，请稍后重试");
        }
    }

    private void ensureMailReady() {
        if (!properties.isMailEnabled()) {
            throw new BizException(ErrorCode.BUSINESS_ERROR, "邮箱验证码服务未开启");
        }
        if (!StringUtils.hasText(properties.getMailFrom())) {
            throw new BizException(ErrorCode.BUSINESS_ERROR, "邮箱发件人未配置");
        }
    }

    private OtpChallenge getChallenge(ChallengeScene scene, String challengeId) {
        if (!StringUtils.hasText(challengeId)) {
            return null;
        }
        String raw = redisTemplate.opsForValue().get(buildChallengeKey(scene, challengeId.trim()));
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, OtpChallenge.class);
        } catch (JsonProcessingException ex) {
            deleteChallenge(scene, challengeId);
            return null;
        }
    }

    private void saveChallenge(String challengeId, OtpChallenge payload) {
        try {
            redisTemplate.opsForValue().set(
                    buildChallengeKey(ChallengeScene.valueOf(payload.getScene()), challengeId),
                    objectMapper.writeValueAsString(payload),
                    Duration.ofSeconds(Math.max(60, properties.getOtpTtlSeconds()))
            );
        } catch (Exception ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "验证码服务暂不可用");
        }
    }

    private void deleteChallenge(ChallengeScene scene, String challengeId) {
        if (StringUtils.hasText(challengeId)) {
            redisTemplate.delete(buildChallengeKey(scene, challengeId.trim()));
        }
    }

    private void saveResetTicket(String ticket, ResetTicketPayload payload) {
        try {
            redisTemplate.opsForValue().set(
                    RESET_TICKET_KEY_PREFIX + ticket,
                    objectMapper.writeValueAsString(payload),
                    Duration.ofSeconds(Math.max(60, properties.getResetTicketTtlSeconds()))
            );
        } catch (Exception ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "重置票据生成失败");
        }
    }

    private ResetTicketPayload consumeResetTicket(String ticket) {
        if (!StringUtils.hasText(ticket)) {
            throw new BizException(ErrorCode.INVALID_PARAM, "重置票据无效");
        }
        String key = RESET_TICKET_KEY_PREFIX + ticket.trim();
        String raw = redisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(raw)) {
            throw new BizException(ErrorCode.INVALID_PARAM, "重置票据无效或已过期");
        }
        redisTemplate.delete(key);
        try {
            return objectMapper.readValue(raw, ResetTicketPayload.class);
        } catch (JsonProcessingException ex) {
            throw new BizException(ErrorCode.INVALID_PARAM, "重置票据无效或已过期");
        }
    }

    private String buildChallengeKey(ChallengeScene scene, String challengeId) {
        return OTP_KEY_PREFIX + scene.name() + ":" + challengeId;
    }

    private String normalizeUsername(String username) {
        return normalizeIdentifier(username);
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return "";
        }
        return email.trim().toLowerCase();
    }

    private String normalizeIdentifier(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().toLowerCase();
    }

    private String normalizeClientIp(String clientIp) {
        if (!StringUtils.hasText(clientIp)) {
            return "unknown";
        }
        return clientIp.trim();
    }

    private User resolveUserByIdentifier(String identifier) {
        if (!StringUtils.hasText(identifier)) {
            return null;
        }
        if (identifier.contains("@")) {
            return userService.findByEmail(identifier);
        }
        return userService.findByUsername(identifier);
    }

    private void validateNewPassword(String newPassword, String confirmPassword) {
        if (!StringUtils.hasText(newPassword) || !StringUtils.hasText(confirmPassword)) {
            throw new BizException(ErrorCode.INVALID_PARAM, "新密码不能为空");
        }
        if (newPassword.length() < 8) {
            throw new BizException(ErrorCode.INVALID_PARAM, "新密码长度至少8位");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new BizException(ErrorCode.INVALID_PARAM, "两次输入的新密码不一致");
        }
    }

    private String generateNumericCode(int length) {
        int safeLength = Math.max(4, Math.min(length, 8));
        StringBuilder code = new StringBuilder(safeLength);
        for (int i = 0; i < safeLength; i++) {
            code.append(secureRandom.nextInt(10));
        }
        return code.toString();
    }

    private String hashCode(String code, String salt) {
        try {
            String merged = (code == null ? "" : code.trim()) + ":" + (salt == null ? "" : salt);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(merged.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception ex) {
            return Integer.toHexString((code + ":" + salt).hashCode());
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception ex) {
            return Integer.toHexString(String.valueOf(value).hashCode());
        }
    }

    private enum ChallengeScene {
        REGISTER,
        CHANGE_PASSWORD,
        FORGOT_PASSWORD,
        BIND_EMAIL
    }

    @Data
    @Builder
    @AllArgsConstructor
    private static class OtpChallenge {
        private String scene;
        private Long userId;
        private String email;
        private String identifier;
        private boolean dummy;
        private String salt;
        private String codeHash;
        private int attempts;
        private int maxAttempts;
        private long createdAt;

        public OtpChallenge() {
        }
    }

    @Data
    @AllArgsConstructor
    private static class ResetTicketPayload {
        private Long userId;
        private String email;

        public ResetTicketPayload() {
        }
    }
}
