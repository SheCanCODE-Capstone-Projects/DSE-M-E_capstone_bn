package com.dseme.app.services.meofficer;

import com.dseme.app.dtos.meofficer.*;
import com.dseme.app.enums.AssignmentAction;
import com.dseme.app.enums.AvailabilityStatus;
import com.dseme.app.enums.CohortStatus;
import com.dseme.app.enums.EnrollmentStatus;
import com.dseme.app.enums.Role;
import com.dseme.app.exceptions.AccessDeniedException;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.models.*;
import com.dseme.app.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for ME_OFFICER facilitator management operations.
 * 
 * Enforces strict partner-level data isolation.
 * All queries filter by partner_id from MEOfficerContext.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MEOfficerFacilitatorService {

    private final UserRepository userRepository;
    private final CohortRepository cohortRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ModuleAssignmentRepository moduleAssignmentRepository;
    private final ScoreRepository scoreRepository;
    private final SurveyResponseRepository surveyResponseRepository;
    private final AuditLogRepository auditLogRepository;
    private final MEOfficerAuthorizationService meOfficerAuthorizationService;
    
    /**
     * Performance threshold for requiring support (average score below this).
     */
    private static final BigDecimal PERFORMANCE_THRESHOLD = new BigDecimal("60.0");
    
    /**
     * Maximum participant load per facilitator.
     */
    private static final int MAX_PARTICIPANT_LOAD = 50;

    /**
     * Gets paginated list of facilitators using advanced search criteria.
     * 
     * @param context ME_OFFICER context
     * @param criteria Search criteria
     * @return Paginated facilitator summary list
     */
    public FacilitatorListPageResponseDTO searchFacilitatorsWithCriteria(
            MEOfficerContext context,
            FacilitatorSearchCriteria criteria
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        // Set partner ID in criteria if not set
        if (criteria.getPartnerId() == null) {
            criteria.setPartnerId(context.getPartnerId());
        }

        // Build pagination
        Sort sort = Sort.by(
                "DESC".equalsIgnoreCase(criteria.getSortDirection()) ? 
                        Sort.Direction.DESC : Sort.Direction.ASC,
                criteria.getSortBy()
        );
        Pageable pageable = PageRequest.of(criteria.getPage(), criteria.getSize(), sort);

        // Build specification
        Specification<User> spec = FacilitatorSpecification.withPartnerAndCriteria(
                context.getPartnerId(), criteria);

        // Query facilitators
        Page<User> facilitatorPage = userRepository.findAll(spec, pageable);

        // Map to summary DTOs with calculated fields
        List<FacilitatorSummaryDTO> summaries = facilitatorPage.getContent().stream()
                .map(facilitator -> mapToFacilitatorSummaryDTO(facilitator, context.getPartnerId(), criteria))
                .filter(summary -> {
                    // Apply post-query filters that require calculations
                    if (criteria.getRequiresSupport() != null) {
                        return summary.getRequiresSupport().equals(criteria.getRequiresSupport());
                    }
                    if (criteria.getMinRating() != null && summary.getFacilitatorRating() != null) {
                        return summary.getFacilitatorRating().compareTo(criteria.getMinRating()) >= 0;
                    }
                    if (criteria.getMaxRating() != null && summary.getFacilitatorRating() != null) {
                        return summary.getFacilitatorRating().compareTo(criteria.getMaxRating()) <= 0;
                    }
                    if (criteria.getAvailabilityStatus() != null) {
                        return summary.getAvailabilityStatus().equals(criteria.getAvailabilityStatus());
                    }
                    return true;
                })
                .collect(Collectors.toList());

        return FacilitatorListPageResponseDTO.builder()
                .content(summaries)
                .totalElements((long) summaries.size())
                .totalPages((int) Math.ceil((double) summaries.size() / criteria.getSize()))
                .currentPage(criteria.getPage())
                .pageSize(criteria.getSize())
                .hasNext(criteria.getPage() < (summaries.size() / criteria.getSize()))
                .hasPrevious(criteria.getPage() > 0)
                .build();
    }

    /**
     * Gets detailed facilitator profile.
     * 
     * @param context ME_OFFICER context
     * @param facilitatorId Facilitator ID (User ID)
     * @return Detailed facilitator profile
     */
    public FacilitatorDetailDTO getFacilitatorProfile(
            MEOfficerContext context,
            UUID facilitatorId
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        // Load facilitator
        User facilitator = userRepository
                .findFacilitatorByIdAndPartnerPartnerId(facilitatorId, Role.FACILITATOR, context.getPartnerId())
                .orElseThrow(() -> {
                    User f = userRepository.findById(facilitatorId).orElse(null);
                    if (f != null && (f.getRole() != Role.FACILITATOR || 
                        f.getPartner() == null || 
                        !f.getPartner().getPartnerId().equals(context.getPartnerId()))) {
                        throw new AccessDeniedException(
                                "Access denied. Facilitator does not belong to your assigned partner."
                        );
                    }
                    return new ResourceNotFoundException(
                            "Facilitator not found with ID: " + facilitatorId
                    );
                });

        // Calculate metrics
        int activeCohortsCount = countActiveCohorts(facilitator);
        int totalParticipants = countTotalParticipants(facilitator, context.getPartnerId());
        BigDecimal averageParticipantScore = calculateAverageParticipantScore(facilitator, context.getPartnerId());
        BigDecimal facilitatorRating = calculateFacilitatorRating(facilitator, context.getPartnerId());
        AvailabilityStatus availabilityStatus = determineAvailabilityStatus(facilitator);
        boolean requiresSupport = averageParticipantScore.compareTo(PERFORMANCE_THRESHOLD) < 0;

        // Get activity logs
        List<ActivityLogDTO> activityLogs = getActivityLogs(facilitator, context.getPartnerId());

        // Get performance trends
        List<MonthlyEngagementDTO> performanceTrends = getPerformanceTrends(facilitator, context.getPartnerId());

        // Build workload metrics
        FacilitatorDetailDTO.WorkloadMetricsDTO workloadMetrics = FacilitatorDetailDTO.WorkloadMetricsDTO.builder()
                .activeCohortsCount(activeCohortsCount)
                .totalParticipants(totalParticipants)
                .maxParticipantLoad(MAX_PARTICIPANT_LOAD)
                .participantLoadPercentage(BigDecimal.valueOf(totalParticipants)
                        .divide(BigDecimal.valueOf(MAX_PARTICIPANT_LOAD), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP))
                .build();

        // Build performance indicators
        FacilitatorDetailDTO.PerformanceIndicatorsDTO performanceIndicators = 
                FacilitatorDetailDTO.PerformanceIndicatorsDTO.builder()
                        .averageParticipantScore(averageParticipantScore)
                        .facilitatorRating(facilitatorRating)
                        .requiresSupport(requiresSupport)
                        .supportReason(requiresSupport ? 
                                String.format("Average participant score (%.2f) is below threshold (%.2f)", 
                                        averageParticipantScore, PERFORMANCE_THRESHOLD) : null)
                        .build();

        // Build contact info
        FacilitatorDetailDTO.ContactInfoDTO contactInfo = FacilitatorDetailDTO.ContactInfoDTO.builder()
                .phone(null) // Not stored in User model currently
                .email(facilitator.getEmail())
                .address(null) // Not stored in User model currently
                .build();

        return FacilitatorDetailDTO.builder()
                .facilitatorId(facilitator.getId())
                .fullName(facilitator.getFirstName() + " " + facilitator.getLastName())
                .email(facilitator.getEmail())
                .profilePictureUrl(null) // Not stored in User model currently
                .specialization(determineSpecialization(facilitator, context.getPartnerId()))
                .yearsOfExperience(calculateYearsOfExperience(facilitator))
                .contactInfo(contactInfo)
                .availabilityStatus(availabilityStatus)
                .activityLogs(activityLogs)
                .performanceTrends(performanceTrends)
                .workloadMetrics(workloadMetrics)
                .performanceIndicators(performanceIndicators)
                .createdAt(facilitator.getCreatedAt())
                .updatedAt(facilitator.getUpdatedAt())
                .build();
    }

    /**
     * Assigns or unassigns facilitator to/from cohorts.
     * 
     * @param context ME_OFFICER context
     * @param request Assignment request
     * @return Assignment response
     */
    @Transactional
    public FacilitatorAssignmentResponseDTO assignFacilitatorToCohorts(
            MEOfficerContext context,
            FacilitatorAssignmentRequestDTO request
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        // Load facilitator
        User facilitator = userRepository
                .findFacilitatorByIdAndPartnerPartnerId(
                        request.getFacilitatorId(), 
                        Role.FACILITATOR, 
                        context.getPartnerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Facilitator not found: " + request.getFacilitatorId()
                ));

        long successful = 0L;
        long failed = 0L;
        List<FacilitatorAssignmentResponseDTO.AssignmentError> errors = new ArrayList<>();

        for (UUID cohortId : request.getCohortIds()) {
            try {
                // Load cohort
                Cohort cohort = cohortRepository.findById(cohortId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Cohort not found: " + cohortId
                        ));

                // Validate cohort belongs to partner
                if (!cohort.getCenter().getPartner().getPartnerId().equals(context.getPartnerId())) {
                    throw new AccessDeniedException(
                            "Access denied. Cohort does not belong to your assigned partner."
                    );
                }

                if (request.getAction() == AssignmentAction.ASSIGN) {
                    // Validate participant load
                    int currentParticipants = countTotalParticipants(facilitator, context.getPartnerId());
                    int cohortParticipants = enrollmentRepository.findByCohortId(cohortId).size();
                    
                    if (currentParticipants + cohortParticipants > MAX_PARTICIPANT_LOAD) {
                        throw new AccessDeniedException(
                                String.format(
                                        "Cannot assign facilitator. Participant load would exceed maximum (%d). " +
                                        "Current: %d, Cohort: %d, Total: %d",
                                        MAX_PARTICIPANT_LOAD, currentParticipants, cohortParticipants,
                                        currentParticipants + cohortParticipants
                                )
                        );
                    }

                    // Assign facilitator to cohort (through module assignments)
                    // Note: Actual assignment happens when ME_OFFICER assigns modules to facilitator
                    // This endpoint validates and logs the intent
                    log.info("Facilitator {} assigned to cohort {} (via module assignments)", 
                            facilitator.getId(), cohortId);
                    successful++;
                } else if (request.getAction() == AssignmentAction.UNASSIGN) {
                    // Unassign facilitator from cohort (remove module assignments)
                    List<ModuleAssignment> assignments = moduleAssignmentRepository
                            .findByFacilitatorIdAndCohortId(facilitator.getId(), cohortId);
                    
                    moduleAssignmentRepository.deleteAll(assignments);
                    
                    log.info("Facilitator {} unassigned from cohort {}", facilitator.getId(), cohortId);
                    successful++;
                }

                // Create audit log
                AuditLog auditLog = AuditLog.builder()
                        .actor(context.getMeOfficer())
                        .actorRole("ME_OFFICER")
                        .action(request.getAction() == AssignmentAction.ASSIGN ? 
                                "ASSIGN_FACILITATOR_TO_COHORT" : "UNASSIGN_FACILITATOR_FROM_COHORT")
                        .entityType("FACILITATOR")
                        .entityId(facilitator.getId())
                        .description(String.format(
                                "ME_OFFICER %s %s facilitator %s %s to/from cohort %s",
                                context.getMeOfficer().getEmail(),
                                request.getAction() == AssignmentAction.ASSIGN ? "assigned" : "unassigned",
                                facilitator.getFirstName(),
                                facilitator.getLastName(),
                                cohort.getCohortName()
                        ))
                        .build();
                auditLogRepository.save(auditLog);

            } catch (Exception e) {
                failed++;
                errors.add(FacilitatorAssignmentResponseDTO.AssignmentError.builder()
                        .cohortId(cohortId)
                        .reason(e.getMessage())
                        .build());
                log.error("Assignment action failed for facilitator {} and cohort {}: {}", 
                        facilitator.getId(), cohortId, e.getMessage());
            }
        }

        String message = String.format(
                "Assignment action '%s' completed: %d successful, %d failed",
                request.getAction(), successful, failed
        );

        return FacilitatorAssignmentResponseDTO.builder()
                .totalRequested((long) request.getCohortIds().size())
                .successful(successful)
                .failed(failed)
                .errors(errors)
                .message(message)
                .build();
    }

    /**
     * Maps User (facilitator) to FacilitatorSummaryDTO with calculated fields.
     */
    private FacilitatorSummaryDTO mapToFacilitatorSummaryDTO(
            User facilitator, 
            String partnerId,
            FacilitatorSearchCriteria criteria
    ) {
        int activeCohortsCount = countActiveCohorts(facilitator);
        int totalParticipants = countTotalParticipants(facilitator, partnerId);
        BigDecimal averageParticipantScore = calculateAverageParticipantScore(facilitator, partnerId);
        BigDecimal facilitatorRating = calculateFacilitatorRating(facilitator, partnerId);
        AvailabilityStatus availabilityStatus = determineAvailabilityStatus(facilitator);
        boolean requiresSupport = averageParticipantScore.compareTo(PERFORMANCE_THRESHOLD) < 0;

        return FacilitatorSummaryDTO.builder()
                .id(facilitator.getId())
                .fullName(facilitator.getFirstName() + " " + facilitator.getLastName())
                .profilePictureUrl(null) // Not stored in User model currently
                .activeCohortsCount(activeCohortsCount)
                .totalParticipants(totalParticipants)
                .averageParticipantScore(averageParticipantScore)
                .facilitatorRating(facilitatorRating)
                .availabilityStatus(availabilityStatus)
                .requiresSupport(requiresSupport)
                .build();
    }

    /**
     * Counts active cohorts for a facilitator.
     */
    private int countActiveCohorts(User facilitator) {
        if (facilitator.getCenter() == null) {
            return 0;
        }
        List<Cohort> activeCohorts = cohortRepository.findByCenterIdAndStatus(
                facilitator.getCenter().getId(), CohortStatus.ACTIVE);
        return activeCohorts.size();
    }

    /**
     * Counts total participants under facilitator's supervision.
     */
    private int countTotalParticipants(User facilitator, String partnerId) {
        if (facilitator.getCenter() == null) {
            return 0;
        }

        // Count participants through module assignments
        List<ModuleAssignment> assignments = moduleAssignmentRepository
                .findByFacilitatorId(facilitator.getId());

        return (int) assignments.stream()
                .flatMap(assignment -> {
                    List<Enrollment> enrollments = enrollmentRepository.findByCohortId(assignment.getCohort().getId());
                    return enrollments.stream()
                            .filter(e -> e.getModule() != null && 
                                       e.getModule().getId().equals(assignment.getModule().getId()))
                            .filter(e -> e.getParticipant().getPartner().getPartnerId().equals(partnerId));
                })
                .distinct()
                .count();
    }

    /**
     * Calculates average participant score for facilitator's students.
     */
    private BigDecimal calculateAverageParticipantScore(User facilitator, String partnerId) {
        // Get all scores recorded by this facilitator
        List<Score> facilitatorScores = scoreRepository.findAll().stream()
                .filter(s -> s.getRecordedBy().getId().equals(facilitator.getId()))
                .filter(s -> s.getEnrollment().getParticipant().getPartner().getPartnerId().equals(partnerId))
                .collect(Collectors.toList());

        if (facilitatorScores.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal sum = facilitatorScores.stream()
                .map(Score::getScoreValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return sum.divide(BigDecimal.valueOf(facilitatorScores.size()), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates facilitator rating from participant feedback surveys.
     * Currently derived from average scores (can be enhanced with actual survey feedback).
     */
    private BigDecimal calculateFacilitatorRating(User facilitator, String partnerId) {
        BigDecimal averageScore = calculateAverageParticipantScore(facilitator, partnerId);
        
        if (averageScore.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(4.5); // Default rating
        }

        // Convert score (0-100) to rating (0-5)
        BigDecimal rating = averageScore.divide(BigDecimal.valueOf(20), 2, RoundingMode.HALF_UP);
        if (rating.compareTo(BigDecimal.valueOf(5)) > 0) {
            rating = BigDecimal.valueOf(5);
        }
        
        return rating;
    }

    /**
     * Determines availability status based on user's active status.
     */
    private AvailabilityStatus determineAvailabilityStatus(User facilitator) {
        if (!Boolean.TRUE.equals(facilitator.getIsActive())) {
            return AvailabilityStatus.INACTIVE;
        }
        // TODO: Add ON_LEAVE status tracking (may require adding field to User model)
        return AvailabilityStatus.ACTIVE;
    }

    /**
     * Determines facilitator specialization from module assignments.
     */
    private String determineSpecialization(User facilitator, String partnerId) {
        List<ModuleAssignment> assignments = moduleAssignmentRepository
                .findByFacilitatorId(facilitator.getId());
        
        if (assignments.isEmpty()) {
            return "General";
        }

        // Get most common program category
        return assignments.stream()
                .map(a -> a.getModule().getProgram().getProgramName())
                .collect(Collectors.groupingBy(p -> p, Collectors.counting()))
                .entrySet().stream()
                .max((a, b) -> Long.compare(a.getValue(), b.getValue()))
                .map(e -> e.getKey())
                .orElse("General");
    }

    /**
     * Calculates years of experience from account creation date.
     */
    private Integer calculateYearsOfExperience(User facilitator) {
        if (facilitator.getCreatedAt() == null) {
            return 0;
        }
        LocalDate createdDate = facilitator.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate();
        return (int) java.time.temporal.ChronoUnit.YEARS.between(createdDate, LocalDate.now());
    }

    /**
     * Gets activity logs for facilitator.
     */
    private List<ActivityLogDTO> getActivityLogs(User facilitator, String partnerId) {
        List<ActivityLogDTO> logs = new ArrayList<>();

        // Get recent scores recorded by facilitator
        List<Score> recentScores = scoreRepository.findAll().stream()
                .filter(s -> s.getRecordedBy().getId().equals(facilitator.getId()))
                .filter(s -> s.getEnrollment().getParticipant().getPartner().getPartnerId().equals(partnerId))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(10)
                .collect(Collectors.toList());

        for (Score score : recentScores) {
            logs.add(ActivityLogDTO.builder()
                    .activityId(score.getId())
                    .activityType("GRADE_UPDATED")
                    .description(String.format("Recorded score %.2f for %s", 
                            score.getScoreValue(), score.getAssessmentName()))
                    .timestamp(score.getCreatedAt())
                    .relatedEntityId(score.getId())
                    .relatedEntityType("SCORE")
                    .build());
        }

        return logs;
    }

    /**
     * Gets performance trends (monthly student engagement).
     */
    private List<MonthlyEngagementDTO> getPerformanceTrends(User facilitator, String partnerId) {
        // Calculate monthly engagement based on attendance or survey responses
        // Simplified implementation - can be enhanced
        List<MonthlyEngagementDTO> trends = new ArrayList<>();
        
        // Get last 6 months
        for (int i = 5; i >= 0; i--) {
            LocalDate monthStart = LocalDate.now().minusMonths(i).withDayOfMonth(1);
            String monthName = monthStart.getMonth().name() + " " + monthStart.getYear();
            
            // Calculate engagement (simplified - based on survey responses)
            long engagementCount = surveyResponseRepository.findAll().stream()
                    .filter(sr -> sr.getParticipant().getPartner().getPartnerId().equals(partnerId))
                    .filter(sr -> {
                        // Check if facilitator is assigned to this participant's cohort
                        List<ModuleAssignment> assignments = moduleAssignmentRepository
                                .findByFacilitatorId(facilitator.getId());
                        return assignments.stream()
                                .anyMatch(a -> a.getCohort().getId().equals(sr.getSurvey().getCohort().getId()));
                    })
                    .filter(sr -> {
                        LocalDate responseDate = sr.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate();
                        return responseDate.getMonth() == monthStart.getMonth() && 
                               responseDate.getYear() == monthStart.getYear();
                    })
                    .count();
            
            trends.add(MonthlyEngagementDTO.builder()
                    .monthName(monthName)
                    .engagementValue(BigDecimal.valueOf(engagementCount))
                    .build());
        }

        return trends;
    }
}
