package com.busgallery.busgallery.service.impl;

import com.busgallery.busgallery.auth.UserRole;
import com.busgallery.busgallery.dto.response.UserProfileResponse;
import com.busgallery.busgallery.entity.Image;
import com.busgallery.busgallery.entity.User;
import com.busgallery.busgallery.mapper.ImageMapper;
import com.busgallery.busgallery.mapper.VehicleCommentMapper;
import com.busgallery.busgallery.exception.BizException;
import com.busgallery.busgallery.exception.ErrorCode;
import com.busgallery.busgallery.repository.UserRepository;
import com.busgallery.busgallery.repository.VehicleSubmissionRepository;
import com.busgallery.busgallery.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * UserServiceImpl类用于封装UserServiceImpl相关的领域职责（所在包：com.busgallery.busgallery.service.impl）。
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ImageMapper imageMapper;
    private final VehicleCommentMapper vehicleCommentMapper;
    private final VehicleSubmissionRepository vehicleSubmissionRepository;

    /**
     * save方法用于处理save相关的业务逻辑。
     * @param user user参数，详见调用方上下文。
     * @return 返回User类型结果。
     */
    @Override
    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    /**
     * existsByUsername方法用于处理existsByUsername相关的业务逻辑。
     * @param username username参数，详见调用方上下文。
     * @return 返回boolean类型结果。
     */
    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return StringUtils.hasText(email) && userRepository.existsByEmail(email.trim().toLowerCase());
    }

    /**
     * findByUsername方法用于处理findByUsername相关的业务逻辑。
     * @param username username参数，详见调用方上下文。
     * @return 返回User类型结果。
     */
    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    @Override
    public User findByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        return userRepository.findByEmail(email.trim().toLowerCase()).orElse(null);
    }

    /**
     * findById方法用于处理findById相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 返回User类型结果。
     */
    @Override
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Override
    public long countUsers() {
        return userRepository.count();
    }

    @Override
    public long countByRole(UserRole role) {
        return userRepository.countByRole(role);
    }

    @Override
    public List<User> listAllUsers() {
        return userRepository.findAllByOrderByIdAsc();
    }

    @Override
    @Transactional
    public User updateRole(Long userId, UserRole role, Long reviewRegionId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return null;
        }
        UserRole nextRole = role == null ? UserRole.USER : role;
        user.setRole(nextRole);
        user.setReviewRegionId(nextRole == UserRole.REVIEWER ? reviewRegionId : null);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User bindEmail(Long userId, String email, LocalDateTime verifiedAt) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return null;
        }
        user.setEmail(StringUtils.hasText(email) ? email.trim().toLowerCase() : null);
        user.setEmailVerifiedAt(verifiedAt);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User updatePassword(Long userId, String passwordHash, LocalDateTime changedAt) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return null;
        }
        user.setPasswordHash(passwordHash);
        user.setPasswordChangedAt(changedAt);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User updateDisplayName(Long userId, String displayName) {
        if (userId == null) {
            return null;
        }
        if (!StringUtils.hasText(displayName)) {
            throw new BizException(ErrorCode.INVALID_PARAM, "昵称不能为空");
        }
        String normalized = displayName.trim();
        if (normalized.length() < 2 || normalized.length() > 128) {
            throw new BizException(ErrorCode.INVALID_PARAM, "昵称长度需在 2-128 个字符之间");
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return null;
        }
        if (normalized.equals(user.getDisplayName())) {
            return user;
        }
        user.setDisplayName(normalized);
        User saved = userRepository.save(user);
        imageMapper.updateUploaderDisplayNameByUploaderId(userId, normalized);
        vehicleCommentMapper.updateDisplayNameByUserId(userId, normalized);
        vehicleSubmissionRepository.updateSubmitterDisplayNameByUserId(userId, normalized);
        vehicleSubmissionRepository.updateReviewerDisplayNameByUserId(userId, normalized);
        return saved;
    }

    /**
     * countUserImages方法用于处理countUserImages相关的业务逻辑。
     * @param userId userId参数，详见调用方上下文。
     * @return 返回long类型结果。
     */
    @Override
    public long countUserImages(Long userId) {
        if (userId == null) {
            return 0L;
        }
        return imageMapper.countByUploader(userId);
    }

    /**
     * listUserImages方法用于处理listUserImages相关的业务逻辑。
     * @param userId userId参数，详见调用方上下文。
     * @param page page参数，详见调用方上下文。
     * @param size size参数，详见调用方上下文。
     * @return 返回List<Image>类型结果。
     */
    @Override
    public List<Image> listUserImages(Long userId, int page, int size) {
        if (userId == null) {
            return Collections.emptyList();
        }
        int pageNo = Math.max(page, 1);
        int pageSize = Math.max(size, 1);
        int offset = (pageNo - 1) * pageSize;
        List<Image> images = imageMapper.selectByUploader(userId, offset, pageSize);
        return CollectionUtils.isEmpty(images) ? Collections.emptyList() : images;
    }

    /**
     * buildProfile方法用于处理buildProfile相关的业务逻辑。
     * @param user user参数，详见调用方上下文。
     * @param uploadsCount uploadsCount参数，详见调用方上下文。
     * @return 返回UserProfileResponse类型结果。
     */
    @Override
    public UserProfileResponse buildProfile(User user, long uploadsCount) {
        if (user == null) {
            return null;
        }
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .emailMasked(maskEmail(user.getEmail()))
                .emailVerified(user.getEmailVerifiedAt() != null)
                .role(user.getRole())
                .reviewRegionId(user.getReviewRegionId())
                .uploadsCount(uploadsCount)
                .build();
    }

    private String maskEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return "";
        }
        String clean = email.trim();
        int at = clean.indexOf('@');
        if (at <= 1) {
            return "***";
        }
        String local = clean.substring(0, at);
        String domain = clean.substring(at);
        if (local.length() <= 2) {
            return local.charAt(0) + "***" + domain;
        }
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }
}
