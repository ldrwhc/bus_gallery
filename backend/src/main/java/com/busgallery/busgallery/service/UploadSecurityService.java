package com.busgallery.busgallery.service;

import com.busgallery.busgallery.auth.UserRole;
import com.busgallery.busgallery.auth.UserSession;
import com.busgallery.busgallery.config.UploadSecurityProperties;
import com.busgallery.busgallery.entity.SubmissionStatus;
import com.busgallery.busgallery.exception.BizException;
import com.busgallery.busgallery.exception.ErrorCode;
import com.busgallery.busgallery.mapper.ImageMapper;
import com.busgallery.busgallery.repository.VehicleSubmissionRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UploadSecurityService {

    private final UploadSecurityProperties properties;
    private final RateLimitService rateLimitService;
    private final ImageMapper imageMapper;
    private final VehicleSubmissionRepository submissionRepository;

    public void checkUploadQuotaAndRate(UserSession session, String clientIp) {
        if (session == null || session.getUserId() == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        String ip = StringUtils.hasText(clientIp) ? clientIp.trim() : "unknown";
        String userKey = String.valueOf(session.getUserId());
        rateLimitService.check("upload", "global", "all", properties.getUploadGlobalPerMinute(), Duration.ofMinutes(1), "上传请求过于频繁，请稍后再试");
        rateLimitService.check("upload", "ip", ip, properties.getUploadIpPerMinute(), Duration.ofMinutes(1), "当前 IP 上传过于频繁，请稍后再试");
        rateLimitService.check("upload", "user", userKey, properties.getUploadUserPerMinute(), Duration.ofMinutes(1), "当前账号上传过于频繁，请稍后再试");

        LocalDateTime start = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        long uploadedToday = imageMapper.countByUploaderBetween(session.getUserId(), start, end);
        if (uploadedToday >= properties.getUploadUserPerDay()) {
            throw new BizException(ErrorCode.REQUEST_DUPLICATE, "今日上传次数已达上限，请明日再试");
        }

        if (session.getRole() == UserRole.USER) {
            long pendingByUser = submissionRepository.countBySubmitterIdAndStatus(session.getUserId(), SubmissionStatus.PENDING);
            if (pendingByUser >= properties.getPendingPerUserMax()) {
                throw new BizException(ErrorCode.REQUEST_DUPLICATE, "待审核数量已达上限，请等待审核后再提交");
            }
        }
        long globalPending = submissionRepository.countByStatus(SubmissionStatus.PENDING);
        if (globalPending >= properties.getPendingGlobalMax()) {
            throw new BizException(ErrorCode.REQUEST_DUPLICATE, "系统待审核队列繁忙，请稍后再试");
        }
    }

    public ValidatedImage validateAndRead(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.INVALID_PARAM, "上传文件不能为空");
        }
        long size = file.getSize();
        if (size <= 0) {
            throw new BizException(ErrorCode.INVALID_PARAM, "上传文件大小无效");
        }
        if (size > properties.getMaxFileBytes()) {
            throw new BizException(ErrorCode.INVALID_PARAM, "上传文件过大");
        }

        String mimeType = normalizeMime(file.getContentType());
        List<String> allowed = properties.getAllowedMimeTypes();
        if (!allowed.contains(mimeType)) {
            throw new BizException(ErrorCode.INVALID_PARAM, "仅支持 JPG/PNG 图片");
        }

        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException ex) {
            throw new BizException(ErrorCode.INVALID_PARAM, "读取上传文件失败");
        }
        if (!magicMatches(data, mimeType)) {
            throw new BizException(ErrorCode.INVALID_PARAM, "文件类型与内容不一致");
        }

        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(data));
        } catch (IOException ex) {
            throw new BizException(ErrorCode.INVALID_PARAM, "图片内容无法解析");
        }
        if (image == null) {
            throw new BizException(ErrorCode.INVALID_PARAM, "图片内容无法解析");
        }
        int width = image.getWidth();
        int height = image.getHeight();
        if (width <= 0 || height <= 0) {
            throw new BizException(ErrorCode.INVALID_PARAM, "图片尺寸无效");
        }
        if (width > properties.getMaxImageWidth() || height > properties.getMaxImageHeight()) {
            throw new BizException(ErrorCode.INVALID_PARAM, "图片分辨率过高");
        }
        long pixels = (long) width * (long) height;
        if (pixels > properties.getMaxImagePixels()) {
            throw new BizException(ErrorCode.INVALID_PARAM, "图片像素总量过大");
        }

        return new ValidatedImage(data, mimeType, width, height);
    }

    private String normalizeMime(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return "";
        }
        return contentType.trim().toLowerCase();
    }

    private boolean magicMatches(byte[] data, String mimeType) {
        if (data == null || data.length < 8) {
            return false;
        }
        if ("image/jpeg".equals(mimeType)) {
            return (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8 && (data[2] & 0xFF) == 0xFF;
        }
        if ("image/png".equals(mimeType)) {
            return (data[0] & 0xFF) == 0x89
                    && data[1] == 0x50
                    && data[2] == 0x4E
                    && data[3] == 0x47
                    && (data[4] & 0xFF) == 0x0D
                    && (data[5] & 0xFF) == 0x0A
                    && (data[6] & 0xFF) == 0x1A
                    && (data[7] & 0xFF) == 0x0A;
        }
        return false;
    }

    @Data
    @AllArgsConstructor
    public static class ValidatedImage {
        private byte[] data;
        private String mimeType;
        private int width;
        private int height;
    }
}
