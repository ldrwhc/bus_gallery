package com.busgallery.busgallery.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 32, message = "用户名长度需在3-32位之间")
    private String username;

    @NotBlank(message = "显示名称不能为空")
    @Size(min = 2, max = 64, message = "显示名称长度需在2-64位之间")
    private String displayName;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度至少6位")
    private String password;

    @NotBlank(message = "请再次确认密码")
    private String confirmPassword;

    @NotBlank(message = "邮箱不能为空")
    @Size(max = 128, message = "邮箱长度不能超过128")
    private String email;

    @NotBlank(message = "验证码挑战ID不能为空")
    private String challengeId;

    @NotBlank(message = "邮箱验证码不能为空")
    @Size(min = 4, max = 10, message = "邮箱验证码格式错误")
    private String emailCode;
}

