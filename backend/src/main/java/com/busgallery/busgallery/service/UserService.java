package com.busgallery.busgallery.service;

import com.busgallery.busgallery.dto.response.UserProfileResponse;
import com.busgallery.busgallery.entity.Image;
import com.busgallery.busgallery.entity.User;

import java.util.List;

public interface UserService {

    User save(User user);

    boolean existsByUsername(String username);

    User findByUsername(String username);

    User findById(Long id);

    long countUserImages(Long userId);

    List<Image> listUserImages(Long userId, int page, int size);

    UserProfileResponse buildProfile(User user, long uploadsCount);
}
