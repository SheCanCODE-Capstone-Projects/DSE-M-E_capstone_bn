package com.dseme.app.services.donor;

import com.dseme.app.dtos.donor.*;
import com.dseme.app.enums.CohortStatus;
import com.dseme.app.enums.EnrollmentStatus;
import com.dseme.app.models.*;
import com.dseme.app.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for DONOR visibility into programs, cohorts, and centers.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DonorVisibilityService {

    private final ProgramRepository programRepository;
    private final CohortRepository cohortRepository;
    private final CenterRepository centerRepository;
    private final EnrollmentRepository enrollmentRepository;

    /**
     * Gets all programs with pagination and filtering.
     */
    public ProgramListResponseDTO getPrograms(
            int page,
            int size,
            String partnerId,
            Boolean isActive
    ) {
        List<Program> allPrograms = programRepository.findAll();
        
        // Apply filters
        List<Program> filtered = allPrograms.stream()
                .filter(p -> partnerId == null || p.getPartner().getPartnerId().equals(partnerId))
                .filter(p -> isActive == null || Boolean.TRUE.equals(p.getIsActive()) == isActive)
                .collect(Collectors.toList());

        // Apply pagination
        int start = page * size;
        int end = Math.min(start + size, filtered.size());
        List<Program> paged = start < filtered.size() ? filtered.subList(start, end) : List.of();

        List<ProgramSummaryDTO> summaries = paged.stream()
                .map(this::mapToProgramSummary)
                .collect(Collectors.toList());

        int totalPages = (int) Math.ceil((double) filtered.size() / size);

        return ProgramListResponseDTO.builder()
                .programs(summaries)
                .totalCount((long) filtered.size())
                .page(page)
                .size(size)
                .totalPages(totalPages)
                .build();
    }

    /**
     * Gets program details by ID.
     */
    public ProgramDetailDTO getProgramById(UUID programId) {
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new com.dseme.app.exceptions.ResourceNotFoundException(
                    "Program with ID '" + programId + "' not found."
                ));

        List<Cohort> cohorts = cohortRepository.findAll().stream()
                .filter(c -> c.getProgram().getId().equals(programId))
                .collect(Collectors.toList());

        List<CohortSummaryDTO> cohortSummaries = cohorts.stream()
                .map(this::mapToCohortSummary)
                .collect(Collectors.toList());

        long totalCohorts = cohorts.size();
        long activeCohorts = cohorts.stream()
                .filter(c -> c.getStatus() == CohortStatus.ACTIVE)
                .count();

        return ProgramDetailDTO.builder()
                .id(program.getId())
                .programName(program.getProgramName())
                .description(program.getDescription())
                .durationWeeks(program.getDurationWeeks())
                .isActive(program.getIsActive())
                .partnerId(program.getPartner().getPartnerId())
                .partnerName(program.getPartner().getPartnerName())
                .totalCohorts(totalCohorts)
                .activeCohorts(activeCohorts)
                .cohorts(cohortSummaries)
                .createdAt(program.getCreatedAt())
                .updatedAt(program.getUpdatedAt())
                .build();
    }

    /**
     * Gets all cohorts with pagination and filtering.
     */
    public CohortListResponseDTO getCohorts(
            int page,
            int size,
            String partnerId,
            UUID programId,
            CohortStatus status
    ) {
        List<Cohort> allCohorts = cohortRepository.findAll();
        
        // Apply filters
        List<Cohort> filtered = allCohorts.stream()
                .filter(c -> partnerId == null || c.getProgram().getPartner().getPartnerId().equals(partnerId))
                .filter(c -> programId == null || c.getProgram().getId().equals(programId))
                .filter(c -> status == null || c.getStatus() == status)
                .collect(Collectors.toList());

        // Apply pagination
        int start = page * size;
        int end = Math.min(start + size, filtered.size());
        List<Cohort> paged = start < filtered.size() ? filtered.subList(start, end) : List.of();

        List<CohortSummaryDTO> summaries = paged.stream()
                .map(this::mapToCohortSummary)
                .collect(Collectors.toList());

        int totalPages = (int) Math.ceil((double) filtered.size() / size);

        return CohortListResponseDTO.builder()
                .cohorts(summaries)
                .totalCount((long) filtered.size())
                .page(page)
                .size(size)
                .totalPages(totalPages)
                .build();
    }

    /**
     * Gets cohort details by ID.
     */
    public CohortDetailDTO getCohortById(UUID cohortId) {
        Cohort cohort = cohortRepository.findById(cohortId)
                .orElseThrow(() -> new com.dseme.app.exceptions.ResourceNotFoundException(
                    "Cohort with ID '" + cohortId + "' not found."
                ));

        // Get enrollments for this cohort
        List<Enrollment> enrollments = enrollmentRepository.findAll().stream()
                .filter(e -> e.getCohort() != null && e.getCohort().getId().equals(cohortId))
                .collect(Collectors.toList());

        long actualEnrollment = enrollments.size();
        long completed = enrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.COMPLETED)
                .count();

        BigDecimal completionRate = actualEnrollment > 0 ?
                BigDecimal.valueOf(completed)
                        .divide(BigDecimal.valueOf(actualEnrollment), 4, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, java.math.RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        return CohortDetailDTO.builder()
                .id(cohort.getId())
                .cohortName(cohort.getCohortName())
                .startDate(cohort.getStartDate())
                .endDate(cohort.getEndDate())
                .status(cohort.getStatus())
                .targetEnrollment(cohort.getTargetEnrollment())
                .actualEnrollment(actualEnrollment)
                .completionRate(completionRate)
                .programId(String.valueOf(cohort.getProgram().getId()))
                .programName(cohort.getProgram().getProgramName())
                .centerId(String.valueOf(cohort.getCenter().getId()))
                .centerName(cohort.getCenter().getCenterName())
                .partnerId(cohort.getProgram().getPartner().getPartnerId())
                .partnerName(cohort.getProgram().getPartner().getPartnerName())
                .createdAt(cohort.getCreatedAt())
                .build();
    }

    /**
     * Gets all centers with pagination and filtering.
     */
    public CenterListResponseDTO getCenters(
            int page,
            int size,
            String partnerId,
            Boolean isActive
    ) {
        List<Center> allCenters = centerRepository.findAll();
        
        // Apply filters
        List<Center> filtered = allCenters.stream()
                .filter(c -> partnerId == null || c.getPartner().getPartnerId().equals(partnerId))
                .filter(c -> isActive == null || Boolean.TRUE.equals(c.getIsActive()) == isActive)
                .collect(Collectors.toList());

        // Apply pagination
        int start = page * size;
        int end = Math.min(start + size, filtered.size());
        List<Center> paged = start < filtered.size() ? filtered.subList(start, end) : List.of();

        List<CenterSummaryDTO> summaries = paged.stream()
                .map(this::mapToCenterSummary)
                .collect(Collectors.toList());

        int totalPages = (int) Math.ceil((double) filtered.size() / size);

        return CenterListResponseDTO.builder()
                .centers(summaries)
                .totalCount((long) filtered.size())
                .page(page)
                .size(size)
                .totalPages(totalPages)
                .build();
    }

    /**
     * Gets center details by ID.
     */
    public CenterDetailDTO getCenterById(UUID centerId) {
        Center center = centerRepository.findById(centerId)
                .orElseThrow(() -> new com.dseme.app.exceptions.ResourceNotFoundException(
                    "Center with ID '" + centerId + "' not found."
                ));

        List<Cohort> cohorts = cohortRepository.findAll().stream()
                .filter(c -> c.getCenter().getId().equals(centerId))
                .collect(Collectors.toList());

        List<CohortSummaryDTO> cohortSummaries = cohorts.stream()
                .map(this::mapToCohortSummary)
                .collect(Collectors.toList());

        long totalCohorts = cohorts.size();
        long activeCohorts = cohorts.stream()
                .filter(c -> c.getStatus() == CohortStatus.ACTIVE)
                .count();

        return CenterDetailDTO.builder()
                .id(center.getId())
                .centerName(center.getCenterName())
                .location(center.getLocation())
                .country(center.getCountry())
                .region(center.getRegion())
                .isActive(center.getIsActive())
                .partnerId(center.getPartner().getPartnerId())
                .partnerName(center.getPartner().getPartnerName())
                .totalCohorts(totalCohorts)
                .activeCohorts(activeCohorts)
                .cohorts(cohortSummaries)
                .createdAt(center.getCreatedAt())
                .build();
    }

    // Mapper methods
    private ProgramSummaryDTO mapToProgramSummary(Program program) {
        List<Cohort> cohorts = cohortRepository.findAll().stream()
                .filter(c -> c.getProgram().getId().equals(program.getId()))
                .collect(Collectors.toList());

        long totalCohorts = cohorts.size();
        long activeCohorts = cohorts.stream()
                .filter(c -> c.getStatus() == CohortStatus.ACTIVE)
                .count();

        return ProgramSummaryDTO.builder()
                .id(program.getId())
                .programName(program.getProgramName())
                .description(program.getDescription())
                .durationWeeks(program.getDurationWeeks())
                .isActive(program.getIsActive())
                .partnerId(program.getPartner().getPartnerId())
                .partnerName(program.getPartner().getPartnerName())
                .totalCohorts(totalCohorts)
                .activeCohorts(activeCohorts)
                .createdAt(program.getCreatedAt())
                .updatedAt(program.getUpdatedAt())
                .build();
    }

    private CohortSummaryDTO mapToCohortSummary(Cohort cohort) {
        List<Enrollment> enrollments = enrollmentRepository.findAll().stream()
                .filter(e -> e.getCohort() != null && e.getCohort().getId().equals(cohort.getId()))
                .collect(Collectors.toList());

        return CohortSummaryDTO.builder()
                .id(cohort.getId())
                .cohortName(cohort.getCohortName())
                .startDate(cohort.getStartDate())
                .endDate(cohort.getEndDate())
                .status(cohort.getStatus())
                .targetEnrollment(cohort.getTargetEnrollment())
                .actualEnrollment((long) enrollments.size())
                .programId(String.valueOf(cohort.getProgram().getId()))
                .programName(cohort.getProgram().getProgramName())
                .centerId(String.valueOf(cohort.getCenter().getId()))
                .centerName(cohort.getCenter().getCenterName())
                .partnerId(cohort.getProgram().getPartner().getPartnerId())
                .partnerName(cohort.getProgram().getPartner().getPartnerName())
                .createdAt(cohort.getCreatedAt())
                .build();
    }

    private CenterSummaryDTO mapToCenterSummary(Center center) {
        List<Cohort> cohorts = cohortRepository.findAll().stream()
                .filter(c -> c.getCenter().getId().equals(center.getId()))
                .collect(Collectors.toList());

        long totalCohorts = cohorts.size();
        long activeCohorts = cohorts.stream()
                .filter(c -> c.getStatus() == CohortStatus.ACTIVE)
                .count();

        return CenterSummaryDTO.builder()
                .id(center.getId())
                .centerName(center.getCenterName())
                .location(center.getLocation())
                .country(center.getCountry())
                .region(center.getRegion())
                .isActive(center.getIsActive())
                .partnerId(center.getPartner().getPartnerId())
                .partnerName(center.getPartner().getPartnerName())
                .totalCohorts(totalCohorts)
                .activeCohorts(activeCohorts)
                .createdAt(center.getCreatedAt())
                .build();
    }
}
