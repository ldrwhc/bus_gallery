package com.busgallery.busgallery.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for password verification.
 */
@Data
public class PasswordVerifyRequest {

    @NotBlank(message = "Current password is required")
    @Size(min = 6, message = "Password length must be at least 6")
    private String currentPassword;
}
