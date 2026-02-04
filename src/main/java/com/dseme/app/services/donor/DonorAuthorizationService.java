package com.dseme.app.services.donor;

import com.dseme.app.dtos.donor.DonorContext;
import com.dseme.app.enums.Role;
import com.dseme.app.exceptions.AccessDeniedException;
import com.dseme.app.models.User;
import com.dseme.app.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for loading DONOR context.
 * 
 * This service:
 * 1. Loads DONOR user from database
 * 2. Validates user has DONOR role
 * 3. Validates user is active and verified
 * 4. Returns context with user information
 * 
 * NOTE: Role validation (DONOR) is also handled by Spring Security.
 * This service provides additional validation and context loading.
 * 
 * Unlike ME_OFFICER, DONOR does not have partnerId restriction
 * as they have portfolio-wide access across all partners.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DonorAuthorizationService {

    private final UserRepository userRepository;

    /**
     * Loads DONOR context from database.
     * 
     * @param email DONOR's email from JWT token
     * @return DonorContext containing user information
     * @throws AccessDeniedException if DONOR context cannot be loaded
     */
    public DonorContext loadDonorContext(String email) {
        // Load user from database
        User donor = userRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("User not found"));

        // Validate user has DONOR role
        if (donor.getRole() != Role.DONOR) {
            throw new AccessDeniedException(
                "Access denied. This endpoint is only accessible to DONOR role. " +
                "Your current role: " + donor.getRole()
            );
        }

        // Validate user is active
        if (!Boolean.TRUE.equals(donor.getIsActive())) {
            throw new AccessDeniedException(
                "Access denied. Your account is not active. Please contact support."
            );
        }

        // Validate user is verified
        if (!Boolean.TRUE.equals(donor.getIsVerified())) {
            throw new AccessDeniedException(
                "Access denied. Your account is not verified. Please verify your email address."
            );
        }

        // Build and return DONOR context
        String fullName = (donor.getFirstName() != null ? donor.getFirstName() : "") +
                         (donor.getLastName() != null ? " " + donor.getLastName() : "").trim();

        return DonorContext.builder()
                .userId(donor.getId())
                .email(donor.getEmail())
                .fullName(fullName.isEmpty() ? donor.getEmail() : fullName)
                .user(donor)
                .build();
    }
}
