package com.dseme.app.services.donor;

import com.dseme.app.dtos.donor.CreatePartnerRequestDTO;
import com.dseme.app.dtos.donor.DonorContext;
import com.dseme.app.dtos.donor.PartnerResponseDTO;
import com.dseme.app.dtos.donor.UpdatePartnerRequestDTO;
import com.dseme.app.enums.Role;
import com.dseme.app.exceptions.ResourceAlreadyExistsException;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.models.AuditLog;
import com.dseme.app.models.Partner;
import com.dseme.app.models.User;
import com.dseme.app.repositories.AuditLogRepository;
import com.dseme.app.repositories.CohortRepository;
import com.dseme.app.repositories.PartnerRepository;
import com.dseme.app.repositories.ProgramRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for DONOR partner organization management.
 * 
 * Provides portfolio-level partner management capabilities:
 * - Create partner organizations
 * - View all partners with metrics
 * 
 * All operations are audit logged.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DonorPartnerService {

    private final PartnerRepository partnerRepository;
    private final ProgramRepository programRepository;
    private final CohortRepository cohortRepository;
    private final AuditLogRepository auditLogRepository;

    /**
     * Creates a new partner organization.
     * 
     * Rules:
     * - Partner name must be unique
     * - Partner created as ACTIVE by default
     * - Audit log entry created
     * 
     * @param context DONOR context
     * @param request Partner creation request
     * @return Created partner response
     * @throws ResourceAlreadyExistsException if partner name already exists
     */
    public PartnerResponseDTO createPartner(DonorContext context, CreatePartnerRequestDTO request) {
        // Check if partner name already exists
        boolean exists = partnerRepository.findAll().stream()
                .anyMatch(p -> p.getPartnerName() != null && 
                        p.getPartnerName().equalsIgnoreCase(request.getPartnerName()));
        
        if (exists) {
            throw new ResourceAlreadyExistsException(
                "Partner organization with name '" + request.getPartnerName() + "' already exists."
            );
        }

        // Generate partner ID (using partner name converted to uppercase, replacing spaces with underscores)
        String partnerId = generatePartnerId(request.getPartnerName());

        // Create partner entity
        Partner partner = new Partner();
        partner.setPartnerId(partnerId);
        partner.setPartnerName(request.getPartnerName());
        partner.setCountry(request.getCountry());
        partner.setRegion(request.getRegion());
        partner.setContactPerson(request.getContactPerson());
        partner.setContactEmail(request.getContactEmail());
        partner.setContactPhone(request.getContactPhone());
        partner.setIsActive(true); // Created as ACTIVE by default
        
        // Save partner
        Partner savedPartner = partnerRepository.save(partner);

        // Create audit log entry
        createAuditLog(
            context.getUser(),
            "CREATE_PARTNER",
            "PARTNER",
            null,
            "Created partner organization: " + savedPartner.getPartnerName()
        );

        // Build and return response
        return mapToPartnerResponseDTO(savedPartner);
    }

    /**
     * Gets all partner organizations with metrics.
     * 
     * Includes:
     * - Partner status (isActive)
     * - Total programs
     * - Total cohorts
     * 
     * @param context DONOR context
     * @return List of partner responses with metrics
     */
    @Transactional(readOnly = true)
    public List<PartnerResponseDTO> getAllPartners(DonorContext context) {
        List<Partner> partners = partnerRepository.findAll();
        
        return partners.stream()
                .map(this::mapToPartnerResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Gets a single partner organization by ID with metrics.
     * 
     * @param context DONOR context
     * @param partnerId Partner ID
     * @return Partner response with metrics
     * @throws ResourceNotFoundException if partner not found
     */
    @Transactional(readOnly = true)
    public PartnerResponseDTO getPartnerById(DonorContext context, String partnerId) {
        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Partner organization with ID '" + partnerId + "' not found."
                ));
        
        return mapToPartnerResponseDTO(partner);
    }

    /**
     * Updates a partner organization.
     * 
     * Rules:
     * - Partner must exist
     * - Partner name must be unique (if changed)
     * - Audit log entry created
     * 
     * @param context DONOR context
     * @param partnerId Partner ID
     * @param request Update request
     * @return Updated partner response
     * @throws ResourceNotFoundException if partner not found
     * @throws ResourceAlreadyExistsException if partner name already exists
     */
    public PartnerResponseDTO updatePartner(DonorContext context, String partnerId, UpdatePartnerRequestDTO request) {
        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Partner organization with ID '" + partnerId + "' not found."
                ));

        // Check if partner name is being changed and if it's unique
        if (request.getPartnerName() != null && 
            !request.getPartnerName().equalsIgnoreCase(partner.getPartnerName())) {
            boolean exists = partnerRepository.findAll().stream()
                    .anyMatch(p -> !p.getPartnerId().equals(partnerId) &&
                            p.getPartnerName() != null &&
                            p.getPartnerName().equalsIgnoreCase(request.getPartnerName()));
            
            if (exists) {
                throw new ResourceAlreadyExistsException(
                    "Partner organization with name '" + request.getPartnerName() + "' already exists."
                );
            }
        }

        // Update fields (only non-null fields)
        if (request.getPartnerName() != null) {
            partner.setPartnerName(request.getPartnerName());
        }
        if (request.getCountry() != null) {
            partner.setCountry(request.getCountry());
        }
        if (request.getRegion() != null) {
            partner.setRegion(request.getRegion());
        }
        if (request.getContactPerson() != null) {
            partner.setContactPerson(request.getContactPerson());
        }
        if (request.getContactEmail() != null) {
            partner.setContactEmail(request.getContactEmail());
        }
        if (request.getContactPhone() != null) {
            partner.setContactPhone(request.getContactPhone());
        }

        // Save updated partner
        Partner updatedPartner = partnerRepository.save(partner);

        // Create audit log entry
        createAuditLog(
            context.getUser(),
            "UPDATE_PARTNER",
            "PARTNER",
            null,
            "Updated partner organization: " + updatedPartner.getPartnerName()
        );

        return mapToPartnerResponseDTO(updatedPartner);
    }

    /**
     * Activates or deactivates a partner organization.
     * 
     * @param context DONOR context
     * @param partnerId Partner ID
     * @param isActive Active status
     * @return Updated partner response
     * @throws ResourceNotFoundException if partner not found
     */
    public PartnerResponseDTO updatePartnerStatus(DonorContext context, String partnerId, Boolean isActive) {
        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Partner organization with ID '" + partnerId + "' not found."
                ));

        partner.setIsActive(isActive);
        Partner updatedPartner = partnerRepository.save(partner);

        // Create audit log entry
        String action = isActive ? "ACTIVATE_PARTNER" : "DEACTIVATE_PARTNER";
        createAuditLog(
            context.getUser(),
            action,
            "PARTNER",
            null,
            (isActive ? "Activated" : "Deactivated") + " partner organization: " + updatedPartner.getPartnerName()
        );

        return mapToPartnerResponseDTO(updatedPartner);
    }

    /**
     * Maps Partner entity to PartnerResponseDTO with metrics.
     */
    private PartnerResponseDTO mapToPartnerResponseDTO(Partner partner) {
        // Calculate metrics
        long totalPrograms = programRepository.countByPartnerPartnerId(partner.getPartnerId());
        long totalCohorts = cohortRepository.countByProgramPartnerPartnerId(partner.getPartnerId());

        return PartnerResponseDTO.builder()
                .partnerId(partner.getPartnerId())
                .partnerName(partner.getPartnerName())
                .country(partner.getCountry())
                .region(partner.getRegion())
                .contactPerson(partner.getContactPerson())
                .contactEmail(partner.getContactEmail())
                .contactPhone(partner.getContactPhone())
                .isActive(partner.getIsActive())
                .createdAt(partner.getCreatedAt())
                .updatedAt(partner.getUpdatedAt())
                .totalPrograms(totalPrograms)
                .totalCohorts(totalCohorts)
                .build();
    }

    /**
     * Generates a partner ID from partner name.
     * Format: UPPERCASE_NAME_WITH_UNDERSCORES
     */
    private String generatePartnerId(String partnerName) {
        // Convert to uppercase and replace spaces/special chars with underscores
        String partnerId = partnerName.toUpperCase()
                .replaceAll("[^A-Z0-9]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        
        // Ensure uniqueness by appending number if needed
        String baseId = partnerId;
        int counter = 1;
        while (partnerRepository.existsById(partnerId)) {
            partnerId = baseId + "_" + counter;
            counter++;
        }
        
        return partnerId;
    }

    /**
     * Creates an audit log entry.
     */
    private void createAuditLog(User actor, String action, String entityType, UUID entityId, String description) {
        AuditLog auditLog = AuditLog.builder()
                .actor(actor)
                .actorRole(actor.getRole() != null ? actor.getRole().name() : Role.DONOR.name())
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .createdAt(Instant.now())
                .build();
        
        auditLogRepository.save(auditLog);
    }
}
