package com.dseme.app.configurations;

import com.dseme.app.enums.Role;
import com.dseme.app.models.User;
import com.dseme.app.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(2) // Run after RoleEnumMigration
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        createAdminUser();
    }

    private void createAdminUser() {
        String adminEmail = System.getProperty("ADMIN_EMAIL", "admin@dseme.com");
        String adminPassword = System.getProperty("ADMIN_PASSWORD", "Admin@123");
        
        if (userRepository.findByEmail(adminEmail).isPresent()) {
            log.info("Admin user already exists. Skipping creation.");
            return;
        }

        User admin = User.builder()
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .firstName("System")
                .lastName("Administrator")
                .role(Role.ADMIN)
                .isActive(true)
                .isVerified(true)
                .build();

        userRepository.save(admin);
        log.info("✅ Admin user created successfully with email: {}", adminEmail);
        log.info("⚠️ Please change the default password after first login.");
    }
}