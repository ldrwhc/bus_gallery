package com.busgallery.busgallery.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ForgotPasswordResetRequest {

    @NotBlank(message = "重置票据不能为空")
    private String resetTicket;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, message = "新密码长度至少8位")
    private String newPassword;

    @NotBlank(message = "请再次确认新密码")
    private String confirmPassword;
}

