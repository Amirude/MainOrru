package com.ooru.service;

import com.ooru.dto.AuthDtos.*;
import com.ooru.dto.ChangePasswordRequest;
import com.ooru.model.Role;
import com.ooru.model.User;
import com.ooru.repository.UserRepository;
import com.ooru.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final OtpService otpService;

    private static final Set<String> SELF_SERVE_ROLES =
            Set.of("CUSTOMER", "SHOP_OWNER", "DELIVERY_PARTNER");

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       OtpService otpService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.otpService = otpService;
    }

    public void register(RegisterRequest req) {

        if (!SELF_SERVE_ROLES.contains(req.role)) {
            throw new IllegalArgumentException("Invalid role for self-registration");
        }

        if (userRepository.existsByPhone(req.phone)) {
            throw new IllegalStateException("An account with this phone number already exists");
        }

        User user = new User();
        user.setName(req.name);
        user.setPhone(req.phone);
        user.setEmail(req.email);
        user.setPasswordHash(passwordEncoder.encode(req.password));

        if (userRepository.count() == 0) {
            user.setRole(Role.ADMIN);
            user.setPhoneVerified(true);
            userRepository.save(user);
            return;
        }

        user.setRole(Role.valueOf(req.role));
        userRepository.save(user);

        otpService.sendOtp(req.phone);
    }

    public void verifyOtp(OtpVerifyRequest req) {

        if (!otpService.verifyOtp(req.phone, req.otp)) {
            throw new IllegalArgumentException("Incorrect or expired OTP");
        }

        User user = userRepository.findByPhone(req.phone)
                .orElseThrow(() ->
                        new IllegalStateException("No account for this phone number"));

        user.setPhoneVerified(true);
        userRepository.save(user);
    }

    public AuthResponse login(LoginRequest req) {

        User user = userRepository.findByPhone(req.identifier)
                .or(() -> userRepository.findByEmail(req.identifier))
                .orElseThrow(() ->
                        new IllegalArgumentException("Incorrect phone/email or password"));

        if (!passwordEncoder.matches(req.password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Incorrect phone/email or password");
        }

        if (!user.isPhoneVerified()) {
            throw new IllegalStateException("Phone number not verified");
        }

        String token = jwtTokenProvider.generateToken(
                user.getId(),
                user.getRole().name());

        return new AuthResponse(
                token,
                user.getId(),
                user.getName(),
                user.getRole()
        );
    }

    public void forgotPassword(ForgotPasswordRequest req) {

        User user = userRepository.findByPhone(req.identifier)
                .or(() -> userRepository.findByEmail(req.identifier))
                .orElse(null);

        if (user == null) {
            return;
        }

        otpService.sendOtp(user.getPhone());
    }

    public void resetPassword(ResetPasswordRequest req) {

        if (!otpService.verifyOtp(req.phone, req.otp)) {
            throw new IllegalArgumentException("Incorrect or expired OTP");
        }

        User user = userRepository.findByPhone(req.phone)
                .orElseThrow(() ->
                        new IllegalStateException("No account for this phone number"));

        user.setPasswordHash(passwordEncoder.encode(req.newPassword));
        userRepository.save(user);
    }

    public void deleteAccount(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalStateException("User not found"));

        user.setName("Deleted user");
        user.setPhone("deleted-" + userId + "-" + System.currentTimeMillis());
        user.setEmail(null);
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setPhoneVerified(false);
        user.setFcmToken(null);

        userRepository.save(user);
    }

    public void changePassword(Long userId, ChangePasswordRequest req) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalStateException("User not found"));

        if (!passwordEncoder.matches(req.currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(req.newPassword));
        userRepository.save(user);
    }
}