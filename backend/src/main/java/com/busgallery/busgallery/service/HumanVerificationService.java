package com.busgallery.busgallery.service;

import com.busgallery.busgallery.config.AuthSecurityProperties;
import com.busgallery.busgallery.dto.response.CaptchaChallengeResponse;
import com.busgallery.busgallery.exception.BizException;
import com.busgallery.busgallery.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HumanVerificationService {

    private static final String CAPTCHA_KEY_PREFIX = "busgallery:auth:captcha:";
    private static final String RISK_KEY_PREFIX = "busgallery:auth:risk:";
    private static final Set<String> SCENES = Set.of("login", "forgot");
    private static final String CAPTCHA_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AuthSecurityProperties properties;
    private final SecureRandom random = new SecureRandom();

    public CaptchaChallengeResponse issueCaptcha(String scene, String clientIp) {
        ensureCaptchaEnabled();
        String normalizedScene = normalizeScene(scene);
        String ip = normalizeIp(clientIp);
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        String code = generateCode(properties.getCaptchaCodeLength());
        String salt = UUID.randomUUID().toString().replace("-", "");
        CaptchaPayload payload = CaptchaPayload.builder()
                .scene(normalizedScene)
                .ip(ip)
                .salt(salt)
                .codeHash(hash(code, salt))
                .attempts(0)
                .maxAttempts(Math.max(1, properties.getCaptchaMaxAttempts()))
                .build();
        saveCaptcha(normalizedScene, captchaId, payload);
        return CaptchaChallengeResponse.builder()
                .scene(normalizedScene)
                .captchaId(captchaId)
                .imageBase64(renderBase64Image(code))
                .expireInSeconds(properties.getCaptchaTtlSeconds())
                .build();
    }

    public void assertLoginCaptchaIfTriggered(String clientIp, String username, String captchaId, String captchaCode) {
        if (!properties.isCaptchaEnabled()) {
            return;
        }
        String ip = normalizeIp(clientIp);
        String identifier = normalizeIdentifier(username);
        long threshold = Math.max(1, properties.getLoginCaptchaFailureThreshold());
        if (getRisk("login-fail", "ip", ip) >= threshold || getRisk("login-fail", "identifier", identifier) >= threshold) {
            verifyCaptcha("login", ip, captchaId, captchaCode);
        }
    }

    public void onLoginFailed(String clientIp, String username) {
        if (!properties.isCaptchaEnabled()) {
            return;
        }
        String ip = normalizeIp(clientIp);
        String identifier = normalizeIdentifier(username);
        incrementRisk("login-fail", "ip", ip, Duration.ofMinutes(30));
        if (StringUtils.hasText(identifier)) {
            incrementRisk("login-fail", "identifier", identifier, Duration.ofMinutes(30));
        }
    }

    public void onLoginSuccess(String clientIp, String username) {
        if (!properties.isCaptchaEnabled()) {
            return;
        }
        String ip = normalizeIp(clientIp);
        String identifier = normalizeIdentifier(username);
        redisTemplate.delete(riskKey("login-fail", "ip", ip));
        if (StringUtils.hasText(identifier)) {
            redisTemplate.delete(riskKey("login-fail", "identifier", identifier));
        }
    }

    public void assertForgotCaptchaIfTriggered(String clientIp, String usernameOrEmail, String captchaId, String captchaCode) {
        if (!properties.isCaptchaEnabled()) {
            return;
        }
        String ip = normalizeIp(clientIp);
        String identifier = normalizeIdentifier(usernameOrEmail);
        Duration window = Duration.ofMinutes(15);
        long ipCount = incrementRisk("forgot-req", "ip", ip, window);
        long identifierCount = StringUtils.hasText(identifier)
                ? incrementRisk("forgot-req", "identifier", identifier, window)
                : 0L;
        long threshold = Math.max(1, properties.getForgotCaptchaRequestThreshold());
        if (ipCount >= threshold || identifierCount >= threshold) {
            verifyCaptcha("forgot", ip, captchaId, captchaCode);
        }
    }

    public void verifyCaptcha(String scene, String clientIp, String captchaId, String captchaCode) {
        ensureCaptchaEnabled();
        String normalizedScene = normalizeScene(scene);
        String ip = normalizeIp(clientIp);
        if (!StringUtils.hasText(captchaId) || !StringUtils.hasText(captchaCode)) {
            throw new BizException(ErrorCode.INVALID_PARAM, "需要先完成图形验证码");
        }
        CaptchaPayload payload = getCaptcha(normalizedScene, captchaId);
        if (payload == null) {
            throw new BizException(ErrorCode.INVALID_PARAM, "图形验证码无效或已过期");
        }
        if (StringUtils.hasText(payload.getIp()) && !"unknown".equalsIgnoreCase(payload.getIp()) && !payload.getIp().equals(ip)) {
            throw new BizException(ErrorCode.INVALID_PARAM, "图形验证码与当前请求不匹配");
        }
        boolean matched = hash(captchaCode.trim().toUpperCase(), payload.getSalt()).equals(payload.getCodeHash());
        if (!matched) {
            payload.setAttempts(payload.getAttempts() + 1);
            if (payload.getAttempts() >= payload.getMaxAttempts()) {
                deleteCaptcha(normalizedScene, captchaId);
            } else {
                saveCaptcha(normalizedScene, captchaId, payload);
            }
            throw new BizException(ErrorCode.INVALID_PARAM, "图形验证码错误");
        }
        deleteCaptcha(normalizedScene, captchaId);
    }

    private void ensureCaptchaEnabled() {
        if (!properties.isCaptchaEnabled()) {
            throw new BizException(ErrorCode.BUSINESS_ERROR, "图形验证码服务未开启");
        }
    }

    private String normalizeScene(String scene) {
        String value = StringUtils.hasText(scene) ? scene.trim().toLowerCase() : "login";
        if (!SCENES.contains(value)) {
            throw new BizException(ErrorCode.INVALID_PARAM, "不支持的验证码场景");
        }
        return value;
    }

    private String normalizeIp(String clientIp) {
        if (!StringUtils.hasText(clientIp)) {
            return "unknown";
        }
        return clientIp.trim();
    }

    private String normalizeIdentifier(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        return raw.trim().toLowerCase();
    }

    private void saveCaptcha(String scene, String captchaId, CaptchaPayload payload) {
        try {
            redisTemplate.opsForValue().set(
                    captchaKey(scene, captchaId),
                    objectMapper.writeValueAsString(payload),
                    Duration.ofSeconds(Math.max(60, properties.getCaptchaTtlSeconds()))
            );
        } catch (Exception ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "图形验证码服务暂不可用");
        }
    }

    @Nullable
    private CaptchaPayload getCaptcha(String scene, String captchaId) {
        String raw = redisTemplate.opsForValue().get(captchaKey(scene, captchaId));
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, CaptchaPayload.class);
        } catch (JsonProcessingException ex) {
            deleteCaptcha(scene, captchaId);
            return null;
        }
    }

    private void deleteCaptcha(String scene, String captchaId) {
        redisTemplate.delete(captchaKey(scene, captchaId));
    }

    private String captchaKey(String scene, String captchaId) {
        return CAPTCHA_KEY_PREFIX + scene + ":" + captchaId;
    }

    private long incrementRisk(String scope, String dimension, String identity, Duration ttl) {
        if (!StringUtils.hasText(identity)) {
            return 0L;
        }
        String key = riskKey(scope, dimension, identity);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, ttl);
        }
        return count == null ? 0L : count;
    }

    private long getRisk(String scope, String dimension, String identity) {
        if (!StringUtils.hasText(identity)) {
            return 0L;
        }
        String value = redisTemplate.opsForValue().get(riskKey(scope, dimension, identity));
        if (!StringUtils.hasText(value)) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private String riskKey(String scope, String dimension, String identity) {
        return RISK_KEY_PREFIX + scope + ":" + dimension + ":" + sha256(identity);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception ex) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private String generateCode(int length) {
        int safeLength = Math.max(4, Math.min(length, 6));
        StringBuilder code = new StringBuilder(safeLength);
        for (int i = 0; i < safeLength; i++) {
            int index = random.nextInt(CAPTCHA_CHARS.length());
            code.append(CAPTCHA_CHARS.charAt(index));
        }
        return code.toString();
    }

    private String hash(String code, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(((code == null ? "" : code) + ":" + (salt == null ? "" : salt)).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception ex) {
            return Integer.toHexString((code + ":" + salt).hashCode());
        }
    }

    private String renderBase64Image(String code) {
        int width = 138;
        int height = 44;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(245, 248, 255));
            g.fillRect(0, 0, width, height);

            for (int i = 0; i < 8; i++) {
                g.setColor(new Color(180 + random.nextInt(60), 180 + random.nextInt(60), 180 + random.nextInt(60)));
                int x1 = random.nextInt(width);
                int y1 = random.nextInt(height);
                int x2 = random.nextInt(width);
                int y2 = random.nextInt(height);
                g.drawLine(x1, y1, x2, y2);
            }

            g.setFont(new Font("SansSerif", Font.BOLD, 28));
            for (int i = 0; i < code.length(); i++) {
                g.setColor(new Color(40 + random.nextInt(100), 40 + random.nextInt(100), 40 + random.nextInt(100)));
                int x = 16 + i * 28;
                int y = 31 + random.nextInt(6);
                g.drawString(String.valueOf(code.charAt(i)), x, y);
            }

            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                ImageIO.write(image, "png", output);
                return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
            } catch (Exception ex) {
                throw new BizException(ErrorCode.INTERNAL_ERROR, "生成图形验证码失败");
            }
        } finally {
            g.dispose();
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    private static class CaptchaPayload {
        private String scene;
        private String ip;
        private String salt;
        private String codeHash;
        private int attempts;
        private int maxAttempts;

        public CaptchaPayload() {
        }
    }
}
