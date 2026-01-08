package com.dseme.app.services.meofficer;

import com.dseme.app.dtos.meofficer.MEOfficerContext;
import com.dseme.app.enums.Role;
import com.dseme.app.exceptions.AccessDeniedException;
import com.dseme.app.models.User;
import com.dseme.app.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for loading ME_OFFICER context.
 * 
 * This service:
 * 1. Loads ME_OFFICER user from database
 * 2. Validates user has ME_OFFICER role
 * 3. Validates user is active and verified
 * 4. Ensures user is assigned to a partner
 * 5. Returns context with partnerId for data isolation
 * 
 * NOTE: Role validation (ME_OFFICER) is also handled by Spring Security.
 * This service provides additional validation and context loading.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MEOfficerAuthorizationService {

    private final UserRepository userRepository;

    /**
     * Loads ME_OFFICER context from database.
     * 
     * @param email ME_OFFICER's email from JWT token
     * @return MEOfficerContext containing partnerId and user information
     * @throws AccessDeniedException if ME_OFFICER context cannot be loaded
     */
    public MEOfficerContext loadMEOfficerContext(String email) {
        // Load user from database
        User meOfficer = userRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("User not found"));

        // Validate user has ME_OFFICER role
        if (meOfficer.getRole() != Role.ME_OFFICER) {
            throw new AccessDeniedException(
                "Access denied. This endpoint is only accessible to ME_OFFICER role. " +
                "Your current role: " + meOfficer.getRole()
            );
        }

        // Validate user is active
        if (!Boolean.TRUE.equals(meOfficer.getIsActive())) {
            throw new AccessDeniedException("Access denied. Your account is not active.");
        }

        // Validate user is verified
        if (!Boolean.TRUE.equals(meOfficer.getIsVerified())) {
            throw new AccessDeniedException("Access denied. Please verify your email address.");
        }

        // Validate partner is assigned
        if (meOfficer.getPartner() == null) {
            throw new AccessDeniedException(
                "Access denied. ME_OFFICER must be assigned to a partner."
            );
        }

        String partnerId = meOfficer.getPartner().getPartnerId();

        // Build and return context
        return MEOfficerContext.builder()
                .meOfficer(meOfficer)
                .partnerId(partnerId)
                .partner(meOfficer.getPartner())
                .userId(meOfficer.getId())
                .build();
    }

    /**
     * Validates that ME_OFFICER can only access data belonging to their partner.
     * 
     * @param context ME_OFFICER context
     * @param requestedPartnerId Partner ID from request/entity
     * @throws AccessDeniedException if ME_OFFICER tries to access data outside their partner
     */
    public void validatePartnerAccess(MEOfficerContext context, String requestedPartnerId) {
        if (requestedPartnerId != null && !requestedPartnerId.equals(context.getPartnerId())) {
            throw new AccessDeniedException(
                "Access denied. You can only access data from your assigned partner. " +
                "Requested partner: " + requestedPartnerId + ", Your partner: " + context.getPartnerId()
            );
        }
    }
}

