package com.busgallery.busgallery.controller;

import com.busgallery.busgallery.auth.AuthContextHolder;
import com.busgallery.busgallery.auth.RequireLogin;
import com.busgallery.busgallery.dto.request.LoginRequest;
import com.busgallery.busgallery.dto.request.RegisterRequest;
import com.busgallery.busgallery.dto.response.LoginResponse;
import com.busgallery.busgallery.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * AuthController类用于封装AuthController相关的领域职责（所在包：com.busgallery.busgallery.controller）。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * register方法用于处理register相关的业务逻辑。
     * @param request request参数，详见调用方上下文。
     * @return 返回LoginResponse类型结果。
     */
    @PostMapping("/register")
    public LoginResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    /**
     * login方法用于处理login相关的业务逻辑。
     * @param request request参数，详见调用方上下文。
     * @return 返回LoginResponse类型结果。
     */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /**
     * logout方法用于处理logout相关的业务逻辑。
     * @return 无返回值。
     */
    @PostMapping("/logout")
    @RequireLogin
    public void logout() {
        if (AuthContextHolder.get() != null) {
            authService.logout(AuthContextHolder.get().getToken());
        }
    }
}
