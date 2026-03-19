package com.busgallery.busgallery.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EmailBindSendCodeRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过128")
    private String email;

    @NotBlank(message = "当前密码不能为空")
    @Size(min = 6, message = "密码长度至少6位")
    private String currentPassword;
}

