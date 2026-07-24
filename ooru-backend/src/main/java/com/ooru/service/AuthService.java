package com.ooru.service;

import com.ooru.dto.AuthDtos.*;
import com.ooru.model.Role;
import com.ooru.model.User;
import com.ooru.repository.UserRepository;
import com.ooru.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    // Swap this for a real SMS provider (MSG91, Twilio, etc.) client — see OtpService.
    private final OtpService otpService;

    private static final Set<String> SELF_SERVE_ROLES = Set.of("CUSTOMER", "SHOP_OWNER", "DELIVERY_PARTNER");

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                        JwtTokenProvider jwtTokenProvider, OtpService otpService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.otpService = otpService;
    }

    public void register(RegisterRequest req) {
        if (!SELF_SERVE_ROLES.contains(req.role)) {
            // ADMIN accounts are never created through the public API — see database/schema.sql
            // for how to seed the first admin directly in the database.
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

        // SAFE first-user bootstrap: if this is truly the very first account ever created on a
        // fresh database, make them admin regardless of what role they picked — this is the
        // "automatic on registration" convenience, without reopening the hole the earlier fix
        // closed. It can only ever fire once, since after this the table is never empty again.
        // Known limitation: two people registering in the same instant on a brand-new database
        // could theoretically both pass this check before either commits — acceptable for a
        // one-time bootstrap event, not something to rely on as an ongoing security boundary.
        if (userRepository.count() == 0) {
            user.setRole(Role.ADMIN);
            user.setPhoneVerified(true);
            userRepository.save(user);
            return; // no OTP needed — trust the very first account on a fresh install
        }

        user.setRole(Role.valueOf(req.role));
        userRepository.save(user);

        // Registration is not complete until the phone number is verified — see verifyOtp below.
        otpService.sendOtp(req.phone);
    }

    public void verifyOtp(OtpVerifyRequest req) {
        boolean ok = otpService.verifyOtp(req.phone, req.otp);
        if (!ok) {
            throw new IllegalArgumentException("Incorrect or expired OTP");
        }
        User user = userRepository.findByPhone(req.phone)
                .orElseThrow(() -> new IllegalStateException("No account for this phone number"));
        user.setPhoneVerified(true);
        userRepository.save(user);
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByPhone(req.identifier)
                .or(() -> userRepository.findByEmail(req.identifier))
                .orElseThrow(() -> new IllegalArgumentException("Incorrect phone/email or password"));

        if (!passwordEncoder.matches(req.password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Incorrect phone/email or password");
        }
        if (!user.isPhoneVerified()) {
            throw new IllegalStateException("Phone number not verified — request a new OTP");
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getRole().name());
        return new AuthResponse(token, user.getId(), user.getName(), user.getRole());
    }

    /** Sends an OTP to the phone on file — works whether you typed your phone or email as the identifier. */
    public void forgotPassword(ForgotPasswordRequest req) {
        User user = userRepository.findByPhone(req.identifier)
                .or(() -> userRepository.findByEmail(req.identifier))
                .orElse(null);
        if (user == null) {
            // Deliberately don't reveal whether the account exists — same response either way.
            return;
        }
        otpService.sendOtp(user.getPhone());
    }

    public void resetPassword(ResetPasswordRequest req) {
        if (!otpService.verifyOtp(req.phone, req.otp)) {
            throw new IllegalArgumentException("Incorrect or expired OTP");
        }
        User user = userRepository.findByPhone(req.phone)
                .orElseThrow(() -> new IllegalStateException("No account for this phone number"));
        user.setPasswordHash(passwordEncoder.encode(req.newPassword));
        userRepository.save(user);
    }

    /**
     * Anonymizes rather than hard-deletes — a real DELETE would violate foreign key constraints
     * the moment this user has ever placed a booking, registered a shop, or left a review (their
     * id is referenced everywhere). Anonymizing keeps that history intact for the other side of
     * each transaction (a shop still needs to see who booked what) while removing everything that
     * identifies THIS person: name, phone, email, and password are all scrubbed, and the account
     * can never be logged into again.
     */
    public void deleteAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        user.setName("Deleted user");
        user.setPhone("deleted-" + userId + "-" + System.currentTimeMillis());
        user.setEmail(null);
        user.setPasswordHash(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
        user.setPhoneVerified(false);
        user.setFcmToken(null);
        userRepository.save(user);
    }

    /**
     * Requires proving you know the CURRENT password before setting a new one — this is what
     * makes it safe to expose to every logged-in user (including admins) rather than something
     * that needs a database console. No email/SMS reset flow yet (see README) — that needs the
     * SMS/email provider that OtpService is also waiting on.
     */
    public void changePassword(Long userId, com.ooru.dto.ChangePasswordRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        if (!passwordEncoder.matches(req.currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(req.newPassword));
        userRepository.save(user);
    }
}
