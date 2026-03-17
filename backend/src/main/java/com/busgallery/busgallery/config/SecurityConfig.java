package com.busgallery.busgallery.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * SecurityConfig类用于封装SecurityConfig相关的领域职责（所在包：com.busgallery.busgallery.config）。
 */
@Configuration
public class SecurityConfig {

    /**
     * passwordEncoder方法用于处理passwordEncoder相关的业务逻辑。
     * @return 返回PasswordEncoder类型结果。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
