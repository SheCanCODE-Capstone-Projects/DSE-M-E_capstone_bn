package com.dseme.app.configurations;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(1) // Run before DataInitializer
public class RoleEnumMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            migrateRoleEnumConstraints();
        } catch (Exception e) {
            log.warn("Role enum migration failed or already applied: {}", e.getMessage());
        }
    }

    private void migrateRoleEnumConstraints() {
        log.info("🔄 Starting Role enum migration...");

        // Step 1: Drop CHECK constraint in users table FIRST (before updating data)
        try {
            jdbcTemplate.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check");
            log.info("✅ Dropped users table role constraint");
        } catch (Exception e) {
            log.debug("Users role constraint drop failed (may not exist): {}", e.getMessage());
        }

        // Step 2: Backfill existing PARTNER records to DONOR in users table
        try {
            int usersUpdated = jdbcTemplate.update(
                "UPDATE users SET role = 'DONOR' WHERE role = 'PARTNER'"
            );
            log.info("✅ Updated {} users from PARTNER to DONOR", usersUpdated);
        } catch (Exception e) {
            log.debug("Failed to update users from PARTNER to DONOR: {}", e.getMessage());
        }

        // Step 3: Recreate CHECK constraint in users table with updated values
        try {
            jdbcTemplate.execute(
                "ALTER TABLE users ADD CONSTRAINT users_role_check " +
                "CHECK (role IN ('ADMIN', 'FACILITATOR', 'ME_OFFICER', 'DONOR', 'UNASSIGNED'))"
            );
            log.info("✅ Updated users table role constraint");
        } catch (Exception e) {
            log.debug("Users role constraint update failed: {}", e.getMessage());
        }

        // Step 4: Drop CHECK constraint in role_requests table FIRST
        try {
            jdbcTemplate.execute("ALTER TABLE role_requests DROP CONSTRAINT IF EXISTS check_requested_role");
            log.info("✅ Dropped role_requests table constraint");
        } catch (Exception e) {
            log.debug("Role requests constraint drop failed (may not exist): {}", e.getMessage());
        }

        // Step 5: Backfill existing PARTNER records to DONOR in role_requests table
        try {
            int requestsUpdated = jdbcTemplate.update(
                "UPDATE role_requests SET requested_role = 'DONOR' WHERE requested_role = 'PARTNER'"
            );
            log.info("✅ Updated {} role requests from PARTNER to DONOR", requestsUpdated);
        } catch (Exception e) {
            log.debug("Role requests table may not exist yet: {}", e.getMessage());
        }

        // Step 6: Recreate CHECK constraint in role_requests table
        try {
            jdbcTemplate.execute(
                "ALTER TABLE role_requests ADD CONSTRAINT check_requested_role " +
                "CHECK (requested_role IN ('FACILITATOR', 'ME_OFFICER', 'DONOR'))"
            );
            log.info("✅ Updated role_requests table constraint");
        } catch (Exception e) {
            log.debug("Role requests constraint update failed: {}", e.getMessage());
        }

        log.info("🎉 Role enum migration completed successfully!");
    }
}