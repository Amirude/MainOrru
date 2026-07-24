package com.ooru.controller;

import com.ooru.dto.AuthDtos.*;
import com.ooru.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        authService.register(req);
        return ResponseEntity.ok(Map.of("message", "Registered — enter the OTP sent to your phone to activate your account."));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody OtpVerifyRequest req) {
        authService.verifyOtp(req);
        return ResponseEntity.ok(Map.of("message", "Phone verified — you can log in now."));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    /** Works for any logged-in user, including admins — this is the real fix for "how do I change the seeded admin password". */
    @PatchMapping("/change-password")
    public ResponseEntity<?> changePassword(org.springframework.security.core.Authentication auth,
                                             @Valid @RequestBody com.ooru.dto.ChangePasswordRequest req) {
        Long userId = (Long) auth.getPrincipal();
        authService.changePassword(userId, req);
        return ResponseEntity.ok(Map.of("message", "Password changed"));
    }

    /** Always returns the same message whether or not the account exists — don't leak which phone/emails are registered. */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        authService.forgotPassword(req);
        return ResponseEntity.ok(Map.of("message", "If an account exists, an OTP has been sent to its phone number."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req);
        return ResponseEntity.ok(Map.of("message", "Password reset — you can log in now."));
    }

    /** Anonymizes the account — see AuthService.deleteAccount for why a hard delete isn't offered. */
    @DeleteMapping("/me")
    public ResponseEntity<?> deleteAccount(org.springframework.security.core.Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        authService.deleteAccount(userId);
        return ResponseEntity.ok(Map.of("message", "Account deleted"));
    }
}
