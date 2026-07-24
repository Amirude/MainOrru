package com.ooru.controller;

import com.ooru.model.User;
import com.ooru.repository.UserRepository;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public static class FcmTokenRequest {
        @NotBlank
        public String fcmToken;
    }

    @PatchMapping("/me/fcm-token")
    public ResponseEntity<?> registerFcmToken(Authentication auth, @RequestBody FcmTokenRequest req) {
        Long userId = (Long) auth.getPrincipal();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        user.setFcmToken(req.fcmToken);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Device registered for notifications"));
    }
}
