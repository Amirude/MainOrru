package com.ooru.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * STUB — this does not send any real SMS. It logs the OTP to the server console instead so you
 * can develop and test the flow locally.
 *
 * To go live, replace the body of sendOtp() with a call to a real SMS provider such as MSG91,
 * Twilio Verify, or AWS SNS, using their SDK and your account credentials. Keep verifyOtp()'s
 * shape the same (phone -> code -> boolean) so nothing else in the codebase has to change.
 */
@Service
public class OtpService {

    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private static final long OTP_VALID_MS = 5 * 60 * 1000; // 5 minutes

    private record OtpEntry(String code, Instant expiresAt) {}

    public void sendOtp(String phone) {
        String code = String.valueOf(100000 + random.nextInt(900000));
        otpStore.put(phone, new OtpEntry(code, Instant.now().plusMillis(OTP_VALID_MS)));

        // TODO: replace this line with a real SMS provider call before deploying.
        System.out.printf("[DEV ONLY] OTP for %s is %s (expires in 5 minutes)%n", phone, code);
    }

    public boolean verifyOtp(String phone, String code) {
        OtpEntry entry = otpStore.get(phone);
        if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
            return false;
        }
        boolean matches = entry.code().equals(code);
        if (matches) {
            otpStore.remove(phone);
        }
        return matches;
    }
}
