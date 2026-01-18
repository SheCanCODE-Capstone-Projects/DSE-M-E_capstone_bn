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
        String adminEmail = System.getenv("ADMIN_EMAIL");
        String adminPassword = System.getenv("ADMIN_PASSWORD");
        
        if (adminEmail == null || adminPassword == null) {
            throw new IllegalStateException("ADMIN_EMAIL and ADMIN_PASSWORD must be set");
        }
        
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
        log.info("Admin user created successfully.");
    }
}