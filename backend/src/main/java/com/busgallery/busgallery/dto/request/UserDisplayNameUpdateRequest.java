package com.busgallery.busgallery.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserDisplayNameUpdateRequest {

    @NotBlank(message = "昵称不能为空")
    @Size(min = 2, max = 128, message = "昵称长度需在2到128字符之间")
    private String displayName;
}

