package com.dseme.app.configurations;

import com.dseme.app.enums.Role;
import com.dseme.app.models.User;
import com.dseme.app.repositories.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PersistenceContext
    private final EntityManager entityManager;
    
    @Value("${ADMIN_EMAIL:admin@dseme.com}")
    private String adminEmail;
    
    @Value("${ADMIN_PASSWORD:Admin@123}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        // Safety migration: convert any legacy ADMIN roles in the users table to DONOR
        // This avoids "No enum constant Role.ADMIN" when reading existing users.
        try {
            int updated = entityManager
                    .createNativeQuery("UPDATE users SET role = 'DONOR' WHERE role = 'ADMIN'")
                    .executeUpdate();
            if (updated > 0) {
                log.info("Converted {} legacy ADMIN user(s) to DONOR.", updated);
            }
        } catch (Exception e) {
            log.warn("Failed to normalize legacy ADMIN roles to DONOR. You may still see ADMIN-related errors.", e);
        }

        createBootstrapDonorUser();
    }

    /**
     * Creates an initial DONOR user for bootstrapping the system.
     * This replaces the old ADMIN superuser.
     */
    private void createBootstrapDonorUser() {
        if (userRepository.findByEmail(adminEmail).isPresent()) {
            log.info("Bootstrap donor user already exists. Skipping creation.");
            return;
        }

        User donor = User.builder()
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .firstName("System")
                .lastName("Donor")
                .role(Role.DONOR)
                .isActive(true)
                .isVerified(true)
                .build();

        userRepository.save(donor);
        log.info("✅ Bootstrap DONOR user created successfully:");
        log.info("   Email: {}", adminEmail);
        log.info("   Please change the default password after first login.");
    }
}