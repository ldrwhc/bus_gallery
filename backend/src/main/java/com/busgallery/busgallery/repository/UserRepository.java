package com.busgallery.busgallery.repository;

import com.busgallery.busgallery.auth.UserRole;
import com.busgallery.busgallery.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * UserRepository接口用于封装UserRepository相关的领域职责（所在包：com.busgallery.busgallery.repository）。
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * findByUsername方法用于处理findByUsername相关的业务逻辑。
     * @param username username参数，详见调用方上下文。
     * @return 返回Optional<User>类型结果。
     */
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    /**
     * existsByUsername方法用于处理existsByUsername相关的业务逻辑。
     * @param username username参数，详见调用方上下文。
     * @return 返回boolean类型结果。
     */
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    long countByRole(UserRole role);

    List<User> findAllByOrderByIdAsc();
}
