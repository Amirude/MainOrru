package com.ooru.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);

    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    private static final long OTP_VALID_MS = 5 * 60 * 1000;

    private record OtpEntry(String code, Instant expiresAt) {}

    public void sendOtp(String phone) {
        String code = String.valueOf(100000 + random.nextInt(900000));

        otpStore.put(
                phone,
                new OtpEntry(code, Instant.now().plusMillis(OTP_VALID_MS))
        );

        log.info("==============================================");
        log.info("OTP GENERATED");
        log.info("Phone : {}", phone);
        log.info("OTP   : {}", code);
        log.info("Expires in 5 minutes");
        log.info("==============================================");
    }

    public boolean verifyOtp(String phone, String code) {

        OtpEntry entry = otpStore.get(phone);

        if (entry == null) {
            return false;
        }

        if (Instant.now().isAfter(entry.expiresAt())) {
            otpStore.remove(phone);
            return false;
        }

        if (entry.code().equals(code)) {
            otpStore.remove(phone);
            return true;
        }

        return false;
    }
}