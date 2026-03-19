package com.busgallery.busgallery.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EmailBindConfirmRequest {

    @NotBlank(message = "验证码挑战ID不能为空")
    private String challengeId;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过128")
    private String email;

    @NotBlank(message = "邮箱验证码不能为空")
    @Size(min = 4, max = 10, message = "邮箱验证码格式错误")
    private String emailCode;
}

