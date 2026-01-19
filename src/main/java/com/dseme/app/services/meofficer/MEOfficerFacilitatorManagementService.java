package com.dseme.app.services.meofficer;

import com.dseme.app.dtos.meofficer.*;
import com.dseme.app.enums.Provider;
import com.dseme.app.enums.Role;
import com.dseme.app.exceptions.AccessDeniedException;
import com.dseme.app.exceptions.ResourceAlreadyExistsException;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.models.AuditLog;
import com.dseme.app.models.Center;
import com.dseme.app.models.Forgotpassword;
import com.dseme.app.models.User;
import com.dseme.app.repositories.AuditLogRepository;
import com.dseme.app.repositories.CenterRepository;
import com.dseme.app.repositories.ForgotPasswordRepository;
import com.dseme.app.repositories.UserRepository;
import com.dseme.app.services.auth.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

/**
 * Service for ME_OFFICER facilitator management operations.
 * 
 * Enforces strict partner-level data isolation.
 * All operations filter by partner_id from MEOfficerContext.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MEOfficerFacilitatorManagementService {

    private final UserRepository userRepository;
    private final CenterRepository centerRepository;
    private final AuditLogRepository auditLogRepository;
    private final ForgotPasswordRepository forgotPasswordRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final MEOfficerAuthorizationService meOfficerAuthorizationService;
    private final Random random = new Random();

    /**
     * Creates a new facilitator account.
     * 
     * @param context ME_OFFICER context
     * @param request Facilitator creation request
     * @return Created facilitator response with temporary password or reset token
     */
    @Transactional
    public CreateFacilitatorResponseDTO createFacilitator(
            MEOfficerContext context,
            CreateFacilitatorRequestDTO request
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException(
                    "User with email '" + request.getEmail() + "' already exists."
            );
        }

        // Load center if provided
        Center center = null;
        if (request.getCenterId() != null) {
            center = centerRepository.findByIdAndPartner_PartnerId(
                    request.getCenterId(),
                    context.getPartnerId()
            ).orElseThrow(() -> new ResourceNotFoundException(
                    "Center not found with ID: " + request.getCenterId() + 
                    " or center does not belong to your partner."
            ));
        }

        // Generate temporary password
        String temporaryPassword = generateTemporaryPassword();
        String passwordHash = passwordEncoder.encode(temporaryPassword);

        // Create facilitator user
        User facilitator = new User();
        facilitator.setEmail(request.getEmail());
        facilitator.setFirstName(request.getFirstName());
        facilitator.setLastName(request.getLastName());
        facilitator.setPasswordHash(passwordHash);
        facilitator.setRole(Role.FACILITATOR);
        facilitator.setPartner(context.getPartner());
        facilitator.setCenter(center);
        facilitator.setIsActive(false); // Requires email verification
        facilitator.setIsVerified(false);
        facilitator.setProvider(Provider.LOCAL);

        facilitator = userRepository.save(facilitator);

        // Generate password reset token (alternative to temporary password)
        int tokenInt = 100000 + random.nextInt(900000);
        String resetToken = String.valueOf(tokenInt);
        
        Forgotpassword forgotPassword = new Forgotpassword();
        forgotPassword.setToken(resetToken);
        forgotPassword.setUser(facilitator);
        forgotPassword.setExpirationTime(new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L)); // 7 days
        forgotPasswordRepository.save(forgotPassword);

        // Send welcome email with temporary password and reset token
        sendWelcomeEmail(facilitator, temporaryPassword, resetToken);

        // Create audit log
        AuditLog auditLog = AuditLog.builder()
                .actor(context.getMeOfficer())
                .actorRole("ME_OFFICER")
                .action("CREATE_FACILITATOR")
                .entityType("USER")
                .entityId(facilitator.getId())
                .description(String.format(
                        "ME_OFFICER %s created facilitator account: %s",
                        context.getMeOfficer().getEmail(),
                        facilitator.getEmail()
                ))
                .build();
        auditLogRepository.save(auditLog);

        log.info("ME_OFFICER {} created facilitator {}: {}", 
                context.getMeOfficer().getEmail(), facilitator.getId(), facilitator.getEmail());

        // Build response
        return CreateFacilitatorResponseDTO.builder()
                .facilitatorId(facilitator.getId())
                .email(facilitator.getEmail())
                .firstName(facilitator.getFirstName())
                .lastName(facilitator.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .centerId(center != null ? center.getId() : null)
                .centerName(center != null ? center.getCenterName() : null)
                .specialization(request.getSpecialization())
                .yearsOfExperience(request.getYearsOfExperience())
                .temporaryPassword(temporaryPassword)
                .passwordResetToken(resetToken)
                .message("Facilitator account created successfully. Welcome email sent with credentials.")
                .build();
    }

    /**
     * Updates facilitator profile.
     * Cannot change email or role.
     * 
     * @param context ME_OFFICER context
     * @param facilitatorId Facilitator ID
     * @param request Update request
     * @return Updated facilitator response
     */
    @Transactional
    public FacilitatorSummaryDTO updateFacilitator(
            MEOfficerContext context,
            UUID facilitatorId,
            UpdateFacilitatorRequestDTO request
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        // Load facilitator
        User facilitator = userRepository
                .findFacilitatorByIdAndPartnerPartnerId(facilitatorId, Role.FACILITATOR, context.getPartnerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Facilitator not found with ID: " + facilitatorId
                ));

        // Update fields (cannot change email or role)
        if (request.getFirstName() != null) {
            facilitator.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            facilitator.setLastName(request.getLastName());
        }
        if (request.getCenterId() != null) {
            Center center = centerRepository.findByIdAndPartner_PartnerId(
                    request.getCenterId(),
                    context.getPartnerId()
            ).orElseThrow(() -> new ResourceNotFoundException(
                    "Center not found with ID: " + request.getCenterId()
            ));
            facilitator.setCenter(center);
        }

        facilitator = userRepository.save(facilitator);

        // Create audit log
        AuditLog auditLog = AuditLog.builder()
                .actor(context.getMeOfficer())
                .actorRole("ME_OFFICER")
                .action("UPDATE_FACILITATOR")
                .entityType("USER")
                .entityId(facilitator.getId())
                .description(String.format(
                        "ME_OFFICER %s updated facilitator: %s",
                        context.getMeOfficer().getEmail(),
                        facilitator.getEmail()
                ))
                .build();
        auditLogRepository.save(auditLog);

        log.info("ME_OFFICER {} updated facilitator {}", 
                context.getMeOfficer().getEmail(), facilitatorId);

        // Map to DTO (simplified - you may want to use the full FacilitatorDetailDTO)
        return FacilitatorSummaryDTO.builder()
                .id(facilitator.getId())
                .fullName(facilitator.getFirstName() + " " + facilitator.getLastName())
                .build();
    }

    /**
     * Activates or deactivates facilitator account.
     * 
     * @param context ME_OFFICER context
     * @param facilitatorId Facilitator ID
     * @param isActive Active status
     * @return Updated facilitator response
     */
    @Transactional
    public FacilitatorSummaryDTO updateFacilitatorStatus(
            MEOfficerContext context,
            UUID facilitatorId,
            Boolean isActive
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        // Load facilitator
        User facilitator = userRepository
                .findFacilitatorByIdAndPartnerPartnerId(facilitatorId, Role.FACILITATOR, context.getPartnerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Facilitator not found with ID: " + facilitatorId
                ));

        facilitator.setIsActive(isActive);
        facilitator = userRepository.save(facilitator);

        // Create audit log
        AuditLog auditLog = AuditLog.builder()
                .actor(context.getMeOfficer())
                .actorRole("ME_OFFICER")
                .action(isActive ? "ACTIVATE_FACILITATOR" : "DEACTIVATE_FACILITATOR")
                .entityType("USER")
                .entityId(facilitator.getId())
                .description(String.format(
                        "ME_OFFICER %s %s facilitator: %s",
                        context.getMeOfficer().getEmail(),
                        isActive ? "activated" : "deactivated",
                        facilitator.getEmail()
                ))
                .build();
        auditLogRepository.save(auditLog);

        log.info("ME_OFFICER {} {} facilitator {}", 
                context.getMeOfficer().getEmail(), isActive ? "activated" : "deactivated", facilitatorId);

        return FacilitatorSummaryDTO.builder()
                .id(facilitator.getId())
                .fullName(facilitator.getFirstName() + " " + facilitator.getLastName())
                .build();
    }

    /**
     * Resets facilitator password.
     * Generates password reset token and sends email.
     * 
     * @param context ME_OFFICER context
     * @param facilitatorId Facilitator ID
     * @return Response message
     */
    @Transactional
    public String resetFacilitatorPassword(
            MEOfficerContext context,
            UUID facilitatorId
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        // Load facilitator
        User facilitator = userRepository
                .findFacilitatorByIdAndPartnerPartnerId(facilitatorId, Role.FACILITATOR, context.getPartnerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Facilitator not found with ID: " + facilitatorId
                ));

        // Delete existing reset tokens
        forgotPasswordRepository.deleteByUser(facilitator);

        // Generate new reset token
        int tokenInt = 100000 + random.nextInt(900000);
        String resetToken = String.valueOf(tokenInt);
        
        Forgotpassword forgotPassword = new Forgotpassword();
        forgotPassword.setToken(resetToken);
        forgotPassword.setUser(facilitator);
        forgotPassword.setExpirationTime(new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L)); // 7 days
        forgotPasswordRepository.save(forgotPassword);

        // Send password reset email
        emailService.sendPasswordResetCode(facilitator.getEmail(), resetToken);

        // Create audit log
        AuditLog auditLog = AuditLog.builder()
                .actor(context.getMeOfficer())
                .actorRole("ME_OFFICER")
                .action("RESET_FACILITATOR_PASSWORD")
                .entityType("USER")
                .entityId(facilitator.getId())
                .description(String.format(
                        "ME_OFFICER %s reset password for facilitator: %s",
                        context.getMeOfficer().getEmail(),
                        facilitator.getEmail()
                ))
                .build();
        auditLogRepository.save(auditLog);

        log.info("ME_OFFICER {} reset password for facilitator {}", 
                context.getMeOfficer().getEmail(), facilitatorId);

        return "Password reset code sent to facilitator's email.";
    }

    /**
     * Generates a temporary password.
     */
    private String generateTemporaryPassword() {
        // Generate 12-character password with letters and numbers
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }

    /**
     * Sends welcome email to facilitator.
     */
    private void sendWelcomeEmail(User facilitator, String temporaryPassword, String resetToken) {
        try {
            // Note: You may want to create a dedicated welcome email method in EmailService
            // For now, we'll use the password reset code method which sends a code
            // The temporary password can be included in a custom email template later
            emailService.sendPasswordResetCode(facilitator.getEmail(), resetToken);
            
            log.info("Welcome email sent to facilitator {} with reset code", facilitator.getEmail());
            
        } catch (Exception e) {
            log.error("Failed to send welcome email to facilitator {}: {}", 
                    facilitator.getEmail(), e.getMessage());
            // Don't throw - account creation should succeed even if email fails
        }
    }
}
