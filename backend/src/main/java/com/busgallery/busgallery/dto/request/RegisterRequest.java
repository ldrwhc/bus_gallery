package com.busgallery.busgallery.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * RegisterRequest类用于封装RegisterRequest相关的领域职责（所在包：com.busgallery.busgallery.dto.request）。
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 32, message = "用户名长度需在 3-32 位之间")
    private String username;

    @NotBlank(message = "显示名称不能为空")
    @Size(min = 2, max = 64, message = "显示名称长度需在 2-64 位之间")
    private String displayName;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度至少 6 位")
    private String password;

    @NotBlank(message = "请再次确认密码")
    private String confirmPassword;
}
