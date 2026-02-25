package com.dseme.app.services.users;

import com.dseme.app.dtos.users.AccessRequestResponseDTO;
import com.dseme.app.dtos.users.RoleRequestDTO;
import com.dseme.app.enums.RequestStatus;
import com.dseme.app.enums.Role;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.models.AccessRequest;
import com.dseme.app.models.User;
import com.dseme.app.models.Partner;
import com.dseme.app.models.Center;
import com.dseme.app.repositories.AccessRequestRepository;
import com.dseme.app.repositories.UserRepository;
import com.dseme.app.repositories.PartnerRepository;
import com.dseme.app.repositories.CenterRepository;
import com.dseme.app.repositories.FacilitatorRepository;
import com.dseme.app.services.auth.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccessRequestService {

    private final AccessRequestRepository accessRequestRepository;
    private final UserRepository userRepository;
    private final PartnerRepository partnerRepository;
    private final CenterRepository centerRepository;
    private final FacilitatorRepository facilitatorRepository;
    private final EmailService emailService;

    @Transactional
    public AccessRequestResponseDTO createRoleRequest(RoleRequestDTO dto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();
        
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Validate user eligibility for role requests
        if (user.getRole() != Role.UNASSIGNED) {
            throw new IllegalStateException("Only UNASSIGNED users can request roles");
        }
        
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new IllegalStateException("User account is not active");
        }
        
        if (!Boolean.TRUE.equals(user.getIsVerified())) {
            throw new IllegalStateException("User email is not verified");
        }

        // Validate requested role based on hierarchy
        Role requestedRole = Role.valueOf(dto.getRequestedRole().toUpperCase());
        if (requestedRole == Role.UNASSIGNED) {
            throw new IllegalArgumentException("Cannot request UNASSIGNED role");
        }
        
        // Only FACILITATOR and ME_OFFICER can be requested via self-service access requests.
        // DONOR users are created and managed directly by the system owner.
        if (requestedRole != Role.FACILITATOR && requestedRole != Role.ME_OFFICER) {
            throw new IllegalArgumentException("Invalid role requested. Only FACILITATOR and ME_OFFICER roles can be requested");
        }
        
        // ME_OFFICER and FACILITATOR MUST have organization and location
        if (dto.getOrganizationPartnerId() == null || dto.getOrganizationPartnerId().trim().isEmpty()) {
            throw new IllegalArgumentException("Organization is required for " + requestedRole + " role. You cannot request this role without specifying an organization.");
        }
        
        if (dto.getLocationCenterId() == null) {
            throw new IllegalArgumentException("Location is required for " + requestedRole + " role. You cannot request this role without specifying a location.");
        }

        // Validate organization (partner) exists
        Partner partner = partnerRepository.findById(dto.getOrganizationPartnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization (partner) not found with ID: " + dto.getOrganizationPartnerId()));

        // Validate location (center) exists and belongs to the organization
        Center center = centerRepository.findByIdAndPartner_PartnerId(
                dto.getLocationCenterId(), 
                dto.getOrganizationPartnerId()
        ).orElseThrow(() -> new ResourceNotFoundException(
                "Location (center) not found or does not belong to the specified organization"));

        AccessRequest request = AccessRequest.builder()
                .requesterEmail(user.getEmail())
                .requesterName(user.getFirstName() + " " + user.getLastName())
                .requestedRole(requestedRole)
                .reason(dto.getReason())
                .organizationPartnerId(dto.getOrganizationPartnerId())
                .locationCenterId(dto.getLocationCenterId())
                .status(RequestStatus.PENDING)
                .build();

        request = accessRequestRepository.save(request);
        return mapToResponseDTO(request);
    }

    public Page<AccessRequestResponseDTO> getAllRequests(Pageable pageable) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String approverEmail = auth.getName();

        User approver = userRepository.findByEmail(approverEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Approver not found"));

        Page<AccessRequest> page = accessRequestRepository.findAll(pageable);

        // Filter based on approver role:
        // - DONOR: sees ME_OFFICER and DONOR requests
        // - ME_OFFICER: sees FACILITATOR requests
        Page<AccessRequest> filteredPage = filterRequestsForApprover(page, approver);
        
        return filteredPage.map(this::mapToResponseDTO);
    }

    public Page<AccessRequestResponseDTO> getPendingRequests(Pageable pageable) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String approverEmail = auth.getName();

        User approver = userRepository.findByEmail(approverEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Approver not found"));

        // Fetch all pending requests first (we'll filter in memory)
        // Note: We fetch all pending to filter properly, then paginate
        Page<AccessRequest> allPending = accessRequestRepository.findByStatus(RequestStatus.PENDING, Pageable.unpaged());

        Page<AccessRequest> filteredPage = filterRequestsForApprover(allPending, approver);
        
        // Apply pagination to filtered results
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredPage.getContent().size());
        List<AccessRequest> paginatedContent = filteredPage.getContent().subList(start, end);
        
        Page<AccessRequest> paginatedPage = new PageImpl<>(
                paginatedContent,
                pageable,
                filteredPage.getTotalElements()
        );

        return paginatedPage.map(this::mapToResponseDTO);
    }

    @Transactional
    public AccessRequestResponseDTO approveRequest(UUID requestId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String approverEmail = auth.getName();
        
        User approver = userRepository.findByEmail(approverEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Approver not found"));

        AccessRequest request = accessRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Access request not found"));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("Request has already been processed");
        }

        // Validate approver is allowed to approve this requested role
        validateApproverForRequestedRole(approver, request);

        // Update the user's role and organization
        User user = userRepository.findByEmail(request.getRequesterEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Requester not found"));
        
        // Granting access means assigning to organization - they are inseparable
        // ME Officers and Facilitators MUST be assigned to an organization when role is granted
        if (request.getRequestedRole() == Role.ME_OFFICER || request.getRequestedRole() == Role.FACILITATOR) {
            if (request.getOrganizationPartnerId() == null || request.getLocationCenterId() == null) {
                throw new IllegalStateException(
                    "Cannot grant " + request.getRequestedRole() + " access without organization and location. " +
                    "Granting this role means the user lives under a specific organization."
                );
            }
            
            // Load and assign organization (partner) and location (center)
            Partner partner = partnerRepository.findById(request.getOrganizationPartnerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
            Center center = centerRepository.findById(request.getLocationCenterId())
                    .orElseThrow(() -> new ResourceNotFoundException("Location not found"));
            
            // Assign organization and location - this is what makes them "live" under the organization
            user.setPartner(partner);
            user.setCenter(center);
        }
        
        // Grant the role - this will trigger validation in User entity
        user.setRole(request.getRequestedRole());
        userRepository.save(user); // Entity validation will ensure ME_OFFICER/FACILITATOR has organization
        
        // Create Facilitator profile if role is FACILITATOR
        if (request.getRequestedRole() == Role.FACILITATOR) {
            createFacilitatorProfile(user);
        }

        // Update the request
        request.setStatus(RequestStatus.APPROVED);
        request.setReviewedAt(Instant.now());
        request.setReviewedBy(approver);

        request = accessRequestRepository.save(request);

        // Send confirmation email to the requester
        try {
            emailService.sendAccessGrantedEmail(
                    request.getRequesterEmail(),
                    request.getRequesterName(),
                    request.getRequestedRole().name()
            );
        } catch (Exception e) {
            // Log but do not fail the approval if email fails
            org.slf4j.LoggerFactory.getLogger(AccessRequestService.class)
                    .warn("Failed to send access-granted email to {}: {}", request.getRequesterEmail(), e.getMessage());
        }

        return mapToResponseDTO(request);
    }

    @Transactional
    public AccessRequestResponseDTO rejectRequest(UUID requestId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String approverEmail = auth.getName();
        
        User approver = userRepository.findByEmail(approverEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Approver not found"));

        AccessRequest request = accessRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Access request not found"));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("Request has already been processed");
        }

        request.setStatus(RequestStatus.REJECTED);
        request.setReviewedAt(Instant.now());
        // We still enforce role responsibility on reject to keep audit clean
        validateApproverForRequestedRole(approver, request);

        request.setReviewedBy(approver);

        request = accessRequestRepository.save(request);
        return mapToResponseDTO(request);
    }

    /**
     * STRICT ORGANIZATION ISOLATION:
     * DONOR: sees ME_OFFICER requests from ALL organizations.
     * ME_OFFICER: sees FACILITATOR requests ONLY from THEIR organization.
     */
    private Page<AccessRequest> filterRequestsForApprover(Page<AccessRequest> page, User approver) {
        Role approverRole = approver.getRole();
        
        List<AccessRequest> filteredContent = page.getContent().stream()
                .filter(request -> {
                    Role requestedRole = request.getRequestedRole();
                    
                    if (approverRole == Role.DONOR) {
                        // DONOR can ONLY see ME_OFFICER requests (all organizations)
                        return requestedRole == Role.ME_OFFICER;
                    } else if (approverRole == Role.ME_OFFICER) {
                        // ME_OFFICER can ONLY see FACILITATOR requests from THEIR organization
                        if (requestedRole != Role.FACILITATOR) {
                            return false;
                        }
                        
                        if (approver.getPartner() == null) {
                            return false;
                        }
                        
                        // STRICT: Must match EXACT organization
                        return approver.getPartner().getPartnerId()
                                .equals(request.getOrganizationPartnerId());
                    }
                    
                    return false;
                })
                .collect(Collectors.toList());
        
        return new PageImpl<>(
                filteredContent,
                page.getPageable(),
                filteredContent.size()
        );
    }

    /**
     * STRICT ORGANIZATION ISOLATION:
     * - DONOR can approve ME_OFFICER requests.
     * - ME_OFFICER can approve FACILITATOR requests ONLY from THEIR organization.
     */
    private void validateApproverForRequestedRole(User approver, AccessRequest request) {
        Role approverRole = approver.getRole();
        Role requestedRole = request.getRequestedRole();

        switch (requestedRole) {
            case ME_OFFICER -> {
                if (approverRole != Role.DONOR) {
                    throw new com.dseme.app.exceptions.AccessDeniedException(
                            "Only DONOR users can approve ME_OFFICER access requests.");
                }
            }
            case FACILITATOR -> {
                if (approverRole != Role.ME_OFFICER) {
                    throw new com.dseme.app.exceptions.AccessDeniedException(
                            "Only ME_OFFICER users can approve FACILITATOR access requests.");
                }
                
                // STRICT: ME_OFFICER can only approve requests for THEIR organization
                if (approver.getPartner() == null) {
                    throw new com.dseme.app.exceptions.AccessDeniedException(
                            "ME_OFFICER must be assigned to an organization to approve requests.");
                }
                
                if (!approver.getPartner().getPartnerId().equals(request.getOrganizationPartnerId())) {
                    throw new com.dseme.app.exceptions.AccessDeniedException(
                            "You can only approve FACILITATOR requests for YOUR organization. This request is for a different organization.");
                }
            }
            case DONOR -> throw new IllegalStateException("DONOR role cannot be requested via access requests");
            case UNASSIGNED -> throw new IllegalStateException("Cannot approve UNASSIGNED role requests");
        }
    }

    private AccessRequestResponseDTO mapToResponseDTO(AccessRequest request) {
        // Load organization and location names
        String organizationName = request.getOrganizationPartnerId() != null
                ? partnerRepository.findById(request.getOrganizationPartnerId())
                        .map(Partner::getPartnerName)
                        .orElse(null)
                : null;
        
        String locationName = request.getLocationCenterId() != null
                ? centerRepository.findById(request.getLocationCenterId())
                        .map(center -> center.getCenterName() + " (" + center.getLocation() + ")")
                        .orElse(null)
                : null;
        
        return AccessRequestResponseDTO.builder()
                .id(request.getId())
                .requesterEmail(request.getRequesterEmail())
                .requesterName(request.getRequesterName())
                .requestedRole(request.getRequestedRole().name())
                .reason(request.getReason())
                .organizationPartnerId(request.getOrganizationPartnerId())
                .locationCenterId(request.getLocationCenterId())
                .organizationName(organizationName)
                .locationName(locationName)
                .status(request.getStatus().name())
                .requestedAt(request.getRequestedAt())
                .reviewedAt(request.getReviewedAt())
                .reviewedBy(request.getReviewedBy() != null ? 
                    request.getReviewedBy().getFirstName() + " " + request.getReviewedBy().getLastName() : null)
                .build();
    }
    
    private void createFacilitatorProfile(User user) {
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AccessRequestService.class);
        logger.info("Creating facilitator profile for user: {}", user.getEmail());
        
        // Check if facilitator profile already exists
        if (facilitatorRepository.findByUserId(user.getId()).isPresent()) {
            logger.info("Facilitator profile already exists for user: {}", user.getEmail());
            return;
        }
        
        // Generate employee ID from email
        String employeeId = "FAC-" + user.getEmail().split("@")[0].toUpperCase();
        
        logger.info("Creating facilitator with employeeId: {}, partner: {}, center: {}",
                employeeId,
                user.getPartner() != null ? user.getPartner().getPartnerName() : "NULL",
                user.getCenter() != null ? user.getCenter().getCenterName() : "NULL");
        
        com.dseme.app.models.Facilitator facilitator = com.dseme.app.models.Facilitator.builder()
                .user(user)
                .employeeId(employeeId)
                .hireDate(java.time.LocalDate.now())
                .build();
        
        facilitator = facilitatorRepository.save(facilitator);
        logger.info("Facilitator profile created successfully with ID: {}", facilitator.getId());
    }
}