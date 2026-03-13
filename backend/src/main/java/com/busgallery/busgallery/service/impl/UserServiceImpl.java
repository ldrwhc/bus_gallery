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

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ImageMapper imageMapper;

    @Override
    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    @Override
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Override
    public long countUserImages(Long userId) {
        if (userId == null) {
            return 0L;
        }
        return imageMapper.countByUploader(userId);
    }

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
