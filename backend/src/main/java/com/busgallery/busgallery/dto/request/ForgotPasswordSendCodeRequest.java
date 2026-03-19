package com.busgallery.busgallery.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ForgotPasswordSendCodeRequest {

    @NotBlank(message = "请输入用户名或邮箱")
    @Size(max = 128, message = "输入长度不能超过 128")
    private String usernameOrEmail;

    private String captchaId;

    private String captchaCode;
}
