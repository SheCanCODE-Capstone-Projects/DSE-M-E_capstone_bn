package com.dseme.app.services.me;

import com.dseme.app.dtos.me.*;
import com.dseme.app.enums.CohortStatus;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.models.*;
import com.dseme.app.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * ME cohort service: each cohort = one course + one (optional) facilitator + participants.
 * Enables "one organization, many cohorts" where each cohort learns a different course
 * with a different facilitator (e.g. She Can Code: Cohort A = Web Dev + Facilitator X,
 * Cohort B = Data Science + Facilitator Y).
 */
@Service
@RequiredArgsConstructor
public class MeCohortService {

    private final MeCohortRepository cohortRepository;
    private final CourseRepository courseRepository;
    private final FacilitatorRepository facilitatorRepository;
    private final UserRepository userRepository;

    public Page<CohortResponseDTO> getAllCohorts(Pageable pageable) {
        User currentUser = getCurrentUser();
        
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MeCohortService.class);
        logger.info("Getting cohorts for user: {} with partner: {}",
                currentUser.getEmail(),
                currentUser.getPartner() != null ? currentUser.getPartner().getPartnerId() : "NULL");
        
        // Filter cohorts by organization (via course.partner)
        if (currentUser.getPartner() != null) {
            Page<MeCohort> cohorts = cohortRepository.findByOrganization(
                    currentUser.getPartner().getPartnerId(),
                    pageable
            );
            logger.info("Found {} cohorts for organization: {}",
                    cohorts.getTotalElements(),
                    currentUser.getPartner().getPartnerName());
            return cohorts.map(this::mapToResponseDTO);
        }
        
        logger.info("User has no partner, returning all cohorts");
        return cohortRepository.findAll(pageable).map(this::mapToResponseDTO);
    }

    public List<CohortResponseDTO> getAllCohortsList() {
        User currentUser = getCurrentUser();
        
        // Filter cohorts by organization
        if (currentUser.getPartner() != null) {
            return cohortRepository.findByOrganizationList(currentUser.getPartner().getPartnerId())
                    .stream()
                    .map(this::mapToResponseDTO)
                    .toList();
        }
        
        return cohortRepository.findAll().stream().map(this::mapToResponseDTO).toList();
    }

    public CohortResponseDTO getCohortById(UUID id) {
        MeCohort cohort = cohortRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cohort not found"));
        return mapToResponseDTO(cohort);
    }

    @Transactional
    public CohortResponseDTO createCohort(CreateCohortDTO dto) {
        User currentUser = getCurrentUser();
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MeCohortService.class);
        
        Course course = courseRepository.findById(dto.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        
        logger.info("Creating cohort for course: {} ({}), Course partner: {}",
                course.getName(),
                course.getId(),
                course.getPartner() != null ? course.getPartner().getPartnerName() : "NULL");
        
        logger.info("Current user partner: {}",
                currentUser.getPartner() != null ? currentUser.getPartner().getPartnerName() : "NULL");

        Facilitator facilitator = null;
        if (dto.getFacilitatorId() != null) {
            facilitator = facilitatorRepository.findById(dto.getFacilitatorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Facilitator not found"));
        }

        MeCohort cohort = MeCohort.builder()
                .name(dto.getName())
                .course(course)
                .facilitator(facilitator)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .maxParticipants(dto.getMaxParticipants() != null && dto.getMaxParticipants() >= 1
                        ? dto.getMaxParticipants() : 30)
                .status(CohortStatus.UPCOMING)
                .build();

        cohort = cohortRepository.save(cohort);
        logger.info("Cohort created successfully with ID: {}", cohort.getId());
        return mapToResponseDTO(cohort);
    }

    @Transactional
    public CohortResponseDTO updateCohort(UUID id, CreateCohortDTO dto) {
        MeCohort cohort = cohortRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cohort not found"));

        Course course = courseRepository.findById(dto.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        Facilitator facilitator = null;
        if (dto.getFacilitatorId() != null) {
            facilitator = facilitatorRepository.findById(dto.getFacilitatorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Facilitator not found"));
        }

        cohort.setName(dto.getName());
        cohort.setCourse(course);
        cohort.setFacilitator(facilitator);
        cohort.setStartDate(dto.getStartDate());
        cohort.setEndDate(dto.getEndDate());
        if (dto.getMaxParticipants() != null && dto.getMaxParticipants() >= 1) {
            cohort.setMaxParticipants(dto.getMaxParticipants());
        }
        cohort = cohortRepository.save(cohort);
        return mapToResponseDTO(cohort);
    }

    @Transactional
    public CohortResponseDTO updateCohortStatus(UUID id, CohortStatus newStatus) {
        MeCohort cohort = cohortRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cohort not found"));
        
        cohort.setStatus(newStatus);
        cohort = cohortRepository.save(cohort);
        
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MeCohortService.class);
        logger.info("Cohort {} status updated to: {}", cohort.getName(), newStatus);
        
        return mapToResponseDTO(cohort);
    }

    @Transactional
    public void deleteCohort(UUID id) {
        MeCohort cohort = cohortRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cohort not found"));
        if (!cohort.getParticipants().isEmpty()) {
            throw new IllegalStateException("Cannot delete cohort with enrolled participants. Remove participants first.");
        }
        cohortRepository.delete(cohort);
    }

    private CohortResponseDTO mapToResponseDTO(MeCohort c) {
        Course course = c.getCourse();
        Facilitator fac = c.getFacilitator();
        MeCohortBatch batch = c.getBatch();
        int current = c.getParticipants() != null ? c.getParticipants().size() : 0;

        return CohortResponseDTO.builder()
                .id(c.getId())
                .name(c.getName())
                .batch(batch != null ? CohortBatchSummaryDTO.builder()
                        .id(batch.getId())
                        .name(batch.getName())
                        .build() : null)
                .course(CourseSummaryDTO.builder()
                        .id(course.getId())
                        .name(course.getName())
                        .code(course.getCode())
                        .level(course.getLevel() != null ? course.getLevel().name() : null)
                        .build())
                .facilitator(fac != null ? FacilitatorSummaryDTO.builder()
                        .id(fac.getId())
                        .firstName(fac.getUser() != null ? fac.getUser().getFirstName() : null)
                        .lastName(fac.getUser() != null ? fac.getUser().getLastName() : null)
                        .build() : null)
                .startDate(c.getStartDate())
                .endDate(c.getEndDate())
                .maxParticipants(c.getMaxParticipants())
                .currentParticipants(current)
                .status(c.getStatus() != null ? c.getStatus().name() : null)
                .build();
    }
    
    private User getCurrentUser() {
        org.springframework.security.core.Authentication auth = 
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
