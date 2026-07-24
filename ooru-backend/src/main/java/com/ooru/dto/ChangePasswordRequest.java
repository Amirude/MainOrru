package com.ooru.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChangePasswordRequest {
    @NotBlank
    public String currentPassword;

    @NotBlank
    @Size(min = 8, message = "New password must be at least 8 characters")
    public String newPassword;
}
