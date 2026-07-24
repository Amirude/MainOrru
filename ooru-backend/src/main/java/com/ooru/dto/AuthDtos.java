package com.ooru.dto;

import com.ooru.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class AuthDtos {

    public static class RegisterRequest {
        @NotBlank
        public String name;

        @NotBlank
        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit Indian mobile number")
        public String phone;

        public String email;

        @NotBlank
        public String password;

        // ADMIN is never accepted from this endpoint — see AuthService for why.
        @NotBlank
        public String role; // "CUSTOMER", "SHOP_OWNER", or "DELIVERY_PARTNER"
    }

    public static class LoginRequest {
        @NotBlank
        public String identifier; // phone number OR email
        @NotBlank
        public String password;
    }

    public static class OtpVerifyRequest {
        @NotBlank
        public String phone;
        @NotBlank
        public String otp;
    }

    public static class ForgotPasswordRequest {
        @NotBlank
        public String identifier; // phone number OR email — the OTP always goes to the phone on file
    }

    public static class ResetPasswordRequest {
        @NotBlank
        public String phone;
        @NotBlank
        public String otp;
        @NotBlank
        public String newPassword;
    }

    public static class AuthResponse {
        public String token;
        public Long userId;
        public String name;
        public Role role;

        public AuthResponse(String token, Long userId, String name, Role role) {
            this.token = token;
            this.userId = userId;
            this.name = name;
            this.role = role;
        }
    }
}
