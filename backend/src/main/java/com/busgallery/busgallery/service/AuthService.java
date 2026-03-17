package com.busgallery.busgallery.service;

import com.busgallery.busgallery.auth.UserSession;
import com.busgallery.busgallery.dto.request.LoginRequest;
import com.busgallery.busgallery.dto.request.RegisterRequest;
import com.busgallery.busgallery.dto.response.LoginResponse;

/**
 * AuthService接口用于封装AuthService相关的领域职责（所在包：com.busgallery.busgallery.service）。
 */
public interface AuthService {

    /**
     * register方法用于处理register相关的业务逻辑。
     * @param request request参数，详见调用方上下文。
     * @return 返回LoginResponse类型结果。
     */
    LoginResponse register(RegisterRequest request);

    /**
     * login方法用于处理login相关的业务逻辑。
     * @param request request参数，详见调用方上下文。
     * @return 返回LoginResponse类型结果。
     */
    LoginResponse login(LoginRequest request);

    /**
     * logout方法用于处理logout相关的业务逻辑。
     * @param token token参数，详见调用方上下文。
     * @return 无返回值。
     */
    void logout(String token);

    /**
     * currentSession方法用于处理currentSession相关的业务逻辑。
     * @return 返回UserSession类型结果。
     */
    UserSession currentSession();
}
