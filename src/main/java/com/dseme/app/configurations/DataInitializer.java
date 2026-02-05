package com.dseme.app.configurations;

import com.dseme.app.enums.Provider;
import com.dseme.app.enums.Role;
import com.dseme.app.models.User;
import com.dseme.app.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(2) // Run after RoleEnumMigration
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Override
    public void run(String... args) {
        createAdminUser();
    }

    private void createAdminUser() {
        String adminEmail = environment.getProperty("ADMIN_EMAIL");
        String adminPassword = environment.getProperty("ADMIN_PASSWORD");
        
        if (!StringUtils.hasText(adminEmail) || !StringUtils.hasText(adminPassword)) {
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
                .provider(Provider.LOCAL)
                .isActive(true)
                .isVerified(true)
                .build();

        userRepository.save(admin);
        log.info("Admin user created successfully.");
    }
}