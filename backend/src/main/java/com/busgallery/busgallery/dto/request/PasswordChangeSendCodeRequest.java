package com.busgallery.busgallery.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordChangeSendCodeRequest {

    @NotBlank(message = "当前密码不能为空")
    @Size(min = 6, message = "密码长度至少6位")
    private String currentPassword;
}

