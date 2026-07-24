package com.ooru.config;

import com.ooru.model.Role;
import com.ooru.model.User;
import com.ooru.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Replaces the old approach of shipping a hardcoded bcrypt hash in database/schema.sql — that
 * hash was the same for everyone who ever cloned this repo, sitting in source control forever,
 * for a password ("change-me-immediately") that was also right there in the same file. That's
 * not really a secret at all.
 *
 * Instead: if no ADMIN account exists yet, and you've set ADMIN_BOOTSTRAP_PHONE and
 * ADMIN_BOOTSTRAP_PASSWORD as real environment variables (never committed to source control),
 * this creates the admin account once, hashing the password for real at startup — the plaintext
 * never touches the database or a file. If those env vars aren't set, this just logs a warning
 * and does nothing; the app still starts fine, you just don't have an admin yet.
 *
 * After the account exists, use PATCH /api/auth/change-password (see AuthController) to change
 * it — there's no need to ever touch the database directly again.
 */
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ooru.admin-bootstrap.name:Platform Admin}")
    private String bootstrapName;

    @Value("${ooru.admin-bootstrap.phone:}")
    private String bootstrapPhone;

    @Value("${ooru.admin-bootstrap.password:}")
    private String bootstrapPassword;

    public AdminBootstrapRunner(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean adminExists = userRepository.findAll().stream().anyMatch(u -> u.getRole() == Role.ADMIN);
        if (adminExists) {
            return; // already bootstrapped in an earlier run — never overwrite an existing admin
        }

        if (bootstrapPhone.isBlank() || bootstrapPassword.isBlank()) {
            log.warn("No admin account exists yet, and ADMIN_BOOTSTRAP_PHONE / ADMIN_BOOTSTRAP_PASSWORD " +
                     "are not set — skipping admin creation. Set both env vars and restart to create one. " +
                     "See README for details.");
            return;
        }
        if (bootstrapPassword.length() < 8) {
            log.error("ADMIN_BOOTSTRAP_PASSWORD is too short (min 8 characters) — refusing to create the admin account.");
            return;
        }

        User admin = new User();
        admin.setName(bootstrapName);
        admin.setPhone(bootstrapPhone);
        admin.setPasswordHash(passwordEncoder.encode(bootstrapPassword));
        admin.setRole(Role.ADMIN);
        admin.setPhoneVerified(true);
        userRepository.save(admin);

        log.info("Created the first admin account for phone {}. Change ADMIN_BOOTSTRAP_PASSWORD or just log in " +
                  "and use PATCH /api/auth/change-password now that the account exists.", bootstrapPhone);
    }
}
