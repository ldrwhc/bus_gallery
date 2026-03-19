package com.busgallery.busgallery.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordChangeConfirmRequest {

    @NotBlank(message = "验证码挑战ID不能为空")
    private String challengeId;

    @NotBlank(message = "邮箱验证码不能为空")
    @Size(min = 4, max = 10, message = "邮箱验证码格式错误")
    private String emailCode;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, message = "新密码长度至少8位")
    private String newPassword;

    @NotBlank(message = "请再次确认新密码")
    private String confirmPassword;
}

