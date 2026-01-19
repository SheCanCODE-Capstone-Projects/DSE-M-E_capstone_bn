package com.dseme.app.services.meofficer;

import com.dseme.app.dtos.meofficer.*;
import com.dseme.app.enums.CohortStatus;
import com.dseme.app.enums.Role;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.models.Center;
import com.dseme.app.repositories.CenterRepository;
import com.dseme.app.repositories.CohortRepository;
import com.dseme.app.repositories.EnrollmentRepository;
import com.dseme.app.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for ME_OFFICER center management operations.
 * 
 * Enforces strict partner-level data isolation.
 * All operations filter by partner_id from MEOfficerContext.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MEOfficerCenterService {

    private final CenterRepository centerRepository;
    private final CohortRepository cohortRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final MEOfficerAuthorizationService meOfficerAuthorizationService;

    /**
     * Gets all centers for the partner with pagination.
     * 
     * @param context ME_OFFICER context
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return Paginated center list
     */
    public CenterListResponseDTO getAllCenters(
            MEOfficerContext context,
            int page,
            int size
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        // Get all centers for partner
        List<Center> centers = centerRepository.findAll().stream()
                .filter(c -> c.getPartner().getPartnerId().equals(context.getPartnerId()))
                .collect(Collectors.toList());
        
        // Manual pagination
        int start = page * size;
        List<Center> pagedCenters = centers.stream()
                .skip(start)
                .limit(size)
                .collect(Collectors.toList());

        List<CenterResponseDTO> centerDTOs = pagedCenters.stream()
                .map(c -> mapToCenterResponseDTO(c, context.getPartnerId()))
                .collect(Collectors.toList());

        return CenterListResponseDTO.builder()
                .centers(centerDTOs)
                .totalElements(centers.size())
                .totalPages((int) Math.ceil((double) centers.size() / size))
                .currentPage(page)
                .pageSize(size)
                .build();
    }

    /**
     * Gets center by ID with detailed information.
     * 
     * @param context ME_OFFICER context
     * @param centerId Center ID
     * @return Center detail DTO
     */
    public CenterDetailDTO getCenterDetail(
            MEOfficerContext context,
            UUID centerId
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        // Load center
        Center center = centerRepository.findByIdAndPartner_PartnerId(centerId, context.getPartnerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Center not found with ID: " + centerId + 
                        " or center does not belong to your partner."
                ));

        // Get cohorts
        List<com.dseme.app.models.Cohort> cohorts = cohortRepository.findByCenterPartnerPartnerId(context.getPartnerId())
                .stream()
                .filter(c -> c.getCenter().getId().equals(centerId))
                .collect(Collectors.toList());

        // Get facilitators
        List<com.dseme.app.models.User> facilitators = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.FACILITATOR)
                .filter(u -> u.getCenter() != null && u.getCenter().getId().equals(centerId))
                .filter(u -> u.getPartner() != null && u.getPartner().getPartnerId().equals(context.getPartnerId()))
                .collect(Collectors.toList());

        // Calculate metrics
        int cohortCount = cohorts.size();
        int activeCohortCount = (int) cohorts.stream()
                .filter(c -> c.getStatus() == CohortStatus.ACTIVE)
                .count();
        int facilitatorCount = facilitators.size();
        
        int participantCount = cohorts.stream()
                .mapToInt(c -> enrollmentRepository.findByCohortId(c.getId()).size())
                .sum();

        // Map cohorts to DTOs
        List<CohortSummaryDTO> cohortDTOs = cohorts.stream()
                .map(c -> CohortSummaryDTO.builder()
                        .cohortId(c.getId())
                        .cohortName(c.getCohortName())
                        .centerName(c.getCenter().getCenterName())
                        .status(c.getStatus())
                        .startDate(c.getStartDate())
                        .endDate(c.getEndDate())
                        .participantCount(enrollmentRepository.findByCohortId(c.getId()).size())
                        .targetEnrollment(c.getTargetEnrollment())
                        .build())
                .collect(Collectors.toList());

        // Map facilitators to DTOs (simplified)
        List<FacilitatorSummaryDTO> facilitatorDTOs = facilitators.stream()
                .map(f -> FacilitatorSummaryDTO.builder()
                        .id(f.getId())
                        .fullName(f.getFirstName() + " " + f.getLastName())
                        .build())
                .collect(Collectors.toList());

        return CenterDetailDTO.builder()
                .centerId(center.getId())
                .centerName(center.getCenterName())
                .location(center.getLocation())
                .country(center.getCountry())
                .region(center.getRegion())
                .isActive(center.getIsActive())
                .cohortCount(cohortCount)
                .activeCohortCount(activeCohortCount)
                .facilitatorCount(facilitatorCount)
                .participantCount(participantCount)
                .cohorts(cohortDTOs)
                .facilitators(facilitatorDTOs)
                .createdAt(center.getCreatedAt())
                .updatedAt(center.getUpdatedAt())
                .build();
    }

    /**
     * Maps Center entity to CenterResponseDTO.
     */
    private CenterResponseDTO mapToCenterResponseDTO(Center center, String partnerId) {
        // Get cohorts for this center
        List<com.dseme.app.models.Cohort> cohorts = cohortRepository.findByCenterPartnerPartnerId(partnerId)
                .stream()
                .filter(c -> c.getCenter().getId().equals(center.getId()))
                .collect(Collectors.toList());

        int cohortCount = cohorts.size();
        int activeCohortCount = (int) cohorts.stream()
                .filter(c -> c.getStatus() == CohortStatus.ACTIVE)
                .count();

        // Get facilitators for this center
        int facilitatorCount = (int) userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.FACILITATOR)
                .filter(u -> u.getCenter() != null && u.getCenter().getId().equals(center.getId()))
                .filter(u -> u.getPartner() != null && u.getPartner().getPartnerId().equals(partnerId))
                .count();

        // Get participant count
        int participantCount = cohorts.stream()
                .mapToInt(c -> enrollmentRepository.findByCohortId(c.getId()).size())
                .sum();

        return CenterResponseDTO.builder()
                .centerId(center.getId())
                .centerName(center.getCenterName())
                .location(center.getLocation())
                .country(center.getCountry())
                .region(center.getRegion())
                .isActive(center.getIsActive())
                .cohortCount(cohortCount)
                .activeCohortCount(activeCohortCount)
                .facilitatorCount(facilitatorCount)
                .participantCount(participantCount)
                .createdAt(center.getCreatedAt())
                .updatedAt(center.getUpdatedAt())
                .build();
    }
}
