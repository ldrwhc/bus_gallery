package com.busgallery.busgallery.service.impl;

import com.busgallery.busgallery.dto.response.UserProfileResponse;
import com.busgallery.busgallery.entity.Image;
import com.busgallery.busgallery.entity.User;
import com.busgallery.busgallery.mapper.ImageMapper;
import com.busgallery.busgallery.repository.UserRepository;
import com.busgallery.busgallery.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

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

    /**
     * findByUsername方法用于处理findByUsername相关的业务逻辑。
     * @param username username参数，详见调用方上下文。
     * @return 返回User类型结果。
     */
    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
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
                .uploadsCount(uploadsCount)
                .build();
    }
}
