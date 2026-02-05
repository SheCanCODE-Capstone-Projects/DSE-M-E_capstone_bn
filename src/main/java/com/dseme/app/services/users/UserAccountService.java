package com.dseme.app.services.users;

import com.dseme.app.dtos.users.*;
import com.dseme.app.enums.Provider;
import com.dseme.app.exceptions.BadRequestException;
import com.dseme.app.exceptions.ResourceAlreadyExistsException;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.models.AuditLog;
import com.dseme.app.models.EmailChangeToken;
import com.dseme.app.models.User;
import com.dseme.app.repositories.AuditLogRepository;
import com.dseme.app.repositories.EmailChangeTokenRepository;
import com.dseme.app.repositories.UserRepository;
import com.dseme.app.services.auth.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for user account management operations.
 * Enforces strict isolation - users can only modify their own accounts.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserAccountService {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final EmailChangeTokenRepository emailChangeTokenRepository;
    private final EmailService emailService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
    
    // Rate limiting for email changes
    private final ConcurrentHashMap<String, Instant> emailChangeRateLimit = new ConcurrentHashMap<>();
    private static final long EMAIL_CHANGE_RATE_LIMIT_MINUTES = 5;

    /**
     * Get current user by email (from JWT).
     * Throws exception if user not found.
     */
    private User getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /**
     * Validate that the user is modifying their own account.
     */
    private void validateOwnership(User user, String requestingEmail) {
        if (!user.getEmail().equals(requestingEmail)) {
            throw new BadRequestException("You can only modify your own account");
        }
    }

    /**
     * Update user profile (firstName, lastName).
     */
    public UserDTO updateProfile(String userEmail, UpdateProfileRequestDTO request) {
        User user = getCurrentUser(userEmail);
        validateOwnership(user, userEmail);

        String oldFirstName = user.getFirstName();
        String oldLastName = user.getLastName();

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user = userRepository.save(user);

        // Audit log
        createAuditLog(user, "UPDATE_PROFILE", "USER", user.getId(),
                String.format("Updated profile: firstName='%s' -> '%s', lastName='%s' -> '%s'",
                        oldFirstName, request.getFirstName(), oldLastName, request.getLastName()));

        return buildUserDTO(user);
    }

    /**
     * Change user password.
     */
    public void changePassword(String userEmail, ChangePasswordRequestDTO request) {
        User user = getCurrentUser(userEmail);
        validateOwnership(user, userEmail);

        // OAuth users cannot change password
        if (user.getProvider() != Provider.LOCAL) {
            throw new BadRequestException("Password change not available for OAuth accounts");
        }

        // Validate current password
        if (user.getPasswordHash() == null || 
            !passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        // Validate new password strength
        validatePasswordStrength(request.getNewPassword());

        // Check if new password is same as current
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password must be different from current password");
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Audit log
        createAuditLog(user, "CHANGE_PASSWORD", "USER", user.getId(),
                "Password changed successfully");
    }

    /**
     * Validate password strength.
     */
    private void validatePasswordStrength(String password) {
        if (password.length() < 8) {
            throw new BadRequestException("Password must be at least 8 characters long");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new BadRequestException("Password must contain at least one uppercase letter");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new BadRequestException("Password must contain at least one lowercase letter");
        }
        if (!password.matches(".*[0-9].*")) {
            throw new BadRequestException("Password must contain at least one number");
        }
    }

    /**
     * Request email change (sends verification email to new address).
     */
    public void requestEmailChange(String userEmail, UpdateEmailRequestDTO request) {
        User user = getCurrentUser(userEmail);
        validateOwnership(user, userEmail);

        // Check if new email is same as current
        if (user.getEmail().equalsIgnoreCase(request.getNewEmail())) {
            throw new BadRequestException("New email must be different from current email");
        }

        // Check if email already exists
        if (userRepository.findByEmail(request.getNewEmail()).isPresent()) {
            throw new ResourceAlreadyExistsException("Email already in use");
        }

        // Rate limiting
        if (isEmailChangeRateLimited(userEmail)) {
            throw new BadRequestException("Please wait 5 minutes before requesting another email change");
        }

        // Delete existing tokens
        emailChangeTokenRepository.deleteByUser(user);
        emailChangeTokenRepository.flush();

        // Generate token
        String token = UUID.randomUUID().toString();
        EmailChangeToken emailChangeToken = EmailChangeToken.builder()
                .token(token)
                .user(user)
                .newEmail(request.getNewEmail())
                .expiryDate(Instant.now().plusSeconds(24 * 60 * 60)) // 24 hours
                .build();

        emailChangeTokenRepository.save(emailChangeToken);

        // Send verification email to new address
        sendEmailChangeVerificationEmail(request.getNewEmail(), token, user.getEmail());

        // Update rate limit
        emailChangeRateLimit.put(userEmail, Instant.now());

        // Audit log
        createAuditLog(user, "REQUEST_EMAIL_CHANGE", "USER", user.getId(),
                String.format("Email change requested: %s -> %s", user.getEmail(), request.getNewEmail()));
    }

    /**
     * Verify and complete email change.
     */
    public void verifyEmailChange(String token) {
        EmailChangeToken emailChangeToken = emailChangeTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid email change token"));

        if (emailChangeToken.isExpired()) {
            emailChangeTokenRepository.delete(emailChangeToken);
            throw new BadRequestException("Email change token has expired");
        }

        User user = emailChangeToken.getUser();
        String oldEmail = user.getEmail();
        String newEmail = emailChangeToken.getNewEmail();

        // Check if new email is still available
        if (userRepository.findByEmail(newEmail).isPresent()) {
            emailChangeTokenRepository.delete(emailChangeToken);
            throw new ResourceAlreadyExistsException("Email is no longer available");
        }

        // Update email
        user.setEmail(newEmail);
        // Reset verification status since email changed
        user.setIsVerified(false);
        userRepository.save(user);

        // Delete token
        emailChangeTokenRepository.delete(emailChangeToken);

        // Send verification email to new address (user needs to verify new email)
        // This will be handled by the existing email verification flow

        // Audit log
        createAuditLog(user, "EMAIL_CHANGED", "USER", user.getId(),
                String.format("Email changed: %s -> %s", oldEmail, newEmail));
    }

    /**
     * Get user account status.
     */
    @Transactional(readOnly = true)
    public AccountStatusDTO getAccountStatus(String userEmail) {
        User user = getCurrentUser(userEmail);
        validateOwnership(user, userEmail);

        return AccountStatusDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .isActive(user.getIsActive())
                .isVerified(user.getIsVerified())
                .provider(user.getProvider().name())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLoginAt(null) // TODO: Add lastLoginAt field to User model if needed
                .build();
    }

    /**
     * Deactivate user account (soft delete).
     */
    public void deactivateAccount(String userEmail) {
        User user = getCurrentUser(userEmail);
        validateOwnership(user, userEmail);

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new BadRequestException("Account is already deactivated");
        }

        user.setIsActive(false);
        userRepository.save(user);

        // Audit log
        createAuditLog(user, "DEACTIVATE_ACCOUNT", "USER", user.getId(),
                "User deactivated their own account");
    }

    /**
     * Get user settings (defaults for now, can be extended with database storage).
     */
    @Transactional(readOnly = true)
    public UserSettingsDTO getSettings(String userEmail) {
        User user = getCurrentUser(userEmail);
        validateOwnership(user, userEmail);

        // Return default settings for now
        // TODO: Store settings in database if needed
        return UserSettingsDTO.builder()
                .emailNotificationsEnabled(true)
                .accountChangeNotifications(true)
                .systemUpdateNotifications(false)
                .build();
    }

    /**
     * Update user settings.
     */
    public UserSettingsDTO updateSettings(String userEmail, UpdateSettingsRequestDTO request) {
        User user = getCurrentUser(userEmail);
        validateOwnership(user, userEmail);

        // TODO: Store settings in database if needed
        // For now, just return updated settings
        UserSettingsDTO settings = UserSettingsDTO.builder()
                .emailNotificationsEnabled(
                        request.getEmailNotificationsEnabled() != null 
                        ? request.getEmailNotificationsEnabled() 
                        : true)
                .accountChangeNotifications(
                        request.getAccountChangeNotifications() != null 
                        ? request.getAccountChangeNotifications() 
                        : true)
                .systemUpdateNotifications(
                        request.getSystemUpdateNotifications() != null 
                        ? request.getSystemUpdateNotifications() 
                        : false)
                .build();

        // Audit log
        createAuditLog(user, "UPDATE_SETTINGS", "USER", user.getId(),
                "User settings updated");

        return settings;
    }

    /**
     * Helper to build UserDTO.
     */
    private UserDTO buildUserDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .isActive(user.getIsActive())
                .isVerified(user.getIsVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * Create audit log entry.
     */
    private void createAuditLog(User actor, String action, String entityType, UUID entityId, String description) {
        AuditLog auditLog = AuditLog.builder()
                .actor(actor)
                .actorRole(actor.getRole().name())
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .createdAt(Instant.now())
                .build();
        auditLogRepository.save(auditLog);
    }

    /**
     * Send email change verification email.
     */
    private void sendEmailChangeVerificationEmail(String newEmail, String token, String oldEmail) {
        try {
            String subject = "Verify Your New Email Address";
            String htmlContent = String.format(
                    "<p>Hello,</p>" +
                    "<p>You have requested to change your email address from <strong>%s</strong> to <strong>%s</strong>.</p>" +
                    "<p>Please verify your new email address by clicking the link below:</p>" +
                    "<p><a href=\"http://localhost:3000/verify-email-change?token=%s\">Verify New Email</a></p>" +
                    "<p>Or copy and paste this link in your browser:</p>" +
                    "<p>http://localhost:3000/verify-email-change?token=%s</p>" +
                    "<p>This link will expire in 24 hours.</p>" +
                    "<p>If you didn't request this change, please ignore this email.</p>" +
                    "<p>Thank you,<br/>DSE Team</p>",
                    oldEmail, newEmail, token, token
            );
            
            // Use EmailService to send email
            // Note: We'll need to add a method to EmailService for this, or use a generic method
            emailService.sendEmail(newEmail, subject, htmlContent);
        } catch (Exception e) {
            log.error("Failed to send email change verification email", e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    /**
     * Check if email change is rate limited.
     */
    private boolean isEmailChangeRateLimited(String email) {
        Instant lastSent = emailChangeRateLimit.get(email);
        if (lastSent == null) {
            return false;
        }
        return Instant.now().isBefore(lastSent.plusSeconds(EMAIL_CHANGE_RATE_LIMIT_MINUTES * 60));
    }
}
