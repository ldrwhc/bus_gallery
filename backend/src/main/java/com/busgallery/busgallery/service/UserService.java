package com.busgallery.busgallery.service;

import com.busgallery.busgallery.auth.UserRole;
import com.busgallery.busgallery.dto.response.UserProfileResponse;
import com.busgallery.busgallery.entity.Image;
import com.busgallery.busgallery.entity.User;

import java.time.LocalDateTime;
import java.util.List;

/**
 * UserService接口用于封装UserService相关的领域职责（所在包：com.busgallery.busgallery.service）。
 */
public interface UserService {

    /**
     * save方法用于处理save相关的业务逻辑。
     * @param user user参数，详见调用方上下文。
     * @return 返回User类型结果。
     */
    User save(User user);

    /**
     * existsByUsername方法用于处理existsByUsername相关的业务逻辑。
     * @param username username参数，详见调用方上下文。
     * @return 返回boolean类型结果。
     */
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /**
     * findByUsername方法用于处理findByUsername相关的业务逻辑。
     * @param username username参数，详见调用方上下文。
     * @return 返回User类型结果。
     */
    User findByUsername(String username);

    User findByEmail(String email);

    /**
     * findById方法用于处理findById相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 返回User类型结果。
     */
    User findById(Long id);

    long countUsers();

    long countByRole(UserRole role);

    List<User> listAllUsers();

    User updateRole(Long userId, UserRole role, Long reviewRegionId);

    User bindEmail(Long userId, String email, LocalDateTime verifiedAt);

    User updatePassword(Long userId, String passwordHash, LocalDateTime changedAt);

    User updateDisplayName(Long userId, String displayName);

    /**
     * countUserImages方法用于处理countUserImages相关的业务逻辑。
     * @param userId userId参数，详见调用方上下文。
     * @return 返回long类型结果。
     */
    long countUserImages(Long userId);

    /**
     * listUserImages方法用于处理listUserImages相关的业务逻辑。
     * @param userId userId参数，详见调用方上下文。
     * @param page page参数，详见调用方上下文。
     * @param size size参数，详见调用方上下文。
     * @return 返回List<Image>类型结果。
     */
    List<Image> listUserImages(Long userId, int page, int size);

    /**
     * buildProfile方法用于处理buildProfile相关的业务逻辑。
     * @param user user参数，详见调用方上下文。
     * @param uploadsCount uploadsCount参数，详见调用方上下文。
     * @return 返回UserProfileResponse类型结果。
     */
    UserProfileResponse buildProfile(User user, long uploadsCount);
}
