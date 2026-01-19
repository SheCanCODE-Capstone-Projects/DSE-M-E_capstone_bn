package com.dseme.app.services.meofficer;

import com.dseme.app.dtos.meofficer.*;
import com.dseme.app.enums.CohortStatus;
import com.dseme.app.enums.EnrollmentStatus;
import com.dseme.app.enums.Role;
import com.dseme.app.models.*;
import com.dseme.app.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for ME_OFFICER dashboard data aggregation.
 * 
 * Provides comprehensive partner-level analytics including:
 * - Summary statistics (8 tiles)
 * - Monthly progress data
 * - Program distribution
 * - Facilitator performance rankings
 * - Cohort status tracking
 * - Course enrollment metrics
 * 
 * All data is restricted to ME_OFFICER's assigned partner.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MEOfficerDashboardService {

    private final ParticipantRepository participantRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CohortRepository cohortRepository;
    private final UserRepository userRepository;
    private final RoleRequestRepository roleRequestRepository;
    private final SurveyResponseRepository surveyResponseRepository;
    private final ScoreRepository scoreRepository;
    private final ProgramRepository programRepository;
    private final TrainingModuleRepository trainingModuleRepository;

    /**
     * Gets comprehensive dashboard data for ME_OFFICER's partner.
     * 
     * @param context ME_OFFICER context
     * @param filterRequest Optional filters (date range, cohort, facilitator)
     * @return MonitoringDashboardResponseDTO with all dashboard data
     */
    public MonitoringDashboardResponseDTO getDashboardData(
            MEOfficerContext context,
            DashboardFilterRequestDTO filterRequest
    ) {
        String partnerId = context.getPartnerId();
        
        // Apply filters
        UUID cohortIdFilter = filterRequest != null ? filterRequest.getCohortId() : null;
        UUID facilitatorIdFilter = filterRequest != null ? filterRequest.getFacilitatorId() : null;
        LocalDate startDateFilter = filterRequest != null ? filterRequest.getStartDate() : null;
        LocalDate endDateFilter = filterRequest != null ? filterRequest.getEndDate() : null;
        
        // Calculate summary stats
        DashboardStatsDTO summaryStats = calculateSummaryStats(partnerId, cohortIdFilter, facilitatorIdFilter);
        
        // Calculate monthly progress
        List<MonthlyDataDTO> monthlyProgress = calculateMonthlyProgress(partnerId, startDateFilter, endDateFilter);
        
        // Calculate program distribution
        OutcomeDistributionDTO programDistribution = calculateProgramDistribution(partnerId, cohortIdFilter);
        
        // Calculate facilitator performance
        List<FacilitatorRankDTO> facilitatorPerformance = calculateFacilitatorPerformance(
                partnerId, facilitatorIdFilter, cohortIdFilter);
        
        // Calculate cohort status
        List<CohortStatusDTO> cohortStatus = calculateCohortStatus(partnerId, cohortIdFilter);
        
        // Calculate course enrollment
        List<CourseMetricDTO> courseEnrollment = calculateCourseEnrollment(partnerId, cohortIdFilter);
        
        return MonitoringDashboardResponseDTO.builder()
                .summaryStats(summaryStats)
                .monthlyProgress(monthlyProgress)
                .programDistribution(programDistribution)
                .facilitatorPerformance(facilitatorPerformance)
                .cohortStatus(cohortStatus)
                .courseEnrollment(courseEnrollment)
                .build();
    }

    /**
     * Calculates summary statistics (8 tiles).
     */
    private DashboardStatsDTO calculateSummaryStats(String partnerId, UUID cohortIdFilter, UUID facilitatorIdFilter) {
        // Total participants
        long totalParticipants = participantRepository.findByPartnerPartnerId(partnerId, 
                org.springframework.data.domain.Pageable.unpaged()).getTotalElements();
        
        // Participant growth (compare current month vs previous month)
        BigDecimal participantGrowth = calculateParticipantGrowth(partnerId);
        
        // Completion rate
        BigDecimal completionRate = calculateCompletionRate(partnerId, cohortIdFilter);
        
        // Average score
        BigDecimal averageScore = calculateAverageScore(partnerId, cohortIdFilter, facilitatorIdFilter);
        
        // Course coverage (e.g., "8/8")
        String courseCoverage = calculateCourseCoverage(partnerId, cohortIdFilter);
        
        // Active facilitators
        int activeFacilitators = countActiveFacilitators(partnerId, facilitatorIdFilter);
        
        // Total cohorts
        List<Cohort> cohorts = cohortIdFilter != null 
                ? Collections.singletonList(cohortRepository.findById(cohortIdFilter).orElse(null))
                : cohortRepository.findByCenterPartnerPartnerId(partnerId);
        cohorts = cohorts.stream().filter(Objects::nonNull).collect(Collectors.toList());
        int totalCohorts = cohorts.size();
        
        // Pending access requests
        long pendingAccessRequests = roleRequestRepository.countPendingByPartnerPartnerId(partnerId);
        
        // Overall survey response rate
        BigDecimal overallSurveyResponseRate = calculateSurveyResponseRate(partnerId, cohortIdFilter);
        
        return DashboardStatsDTO.builder()
                .totalParticipants((int) totalParticipants)
                .participantGrowth(participantGrowth)
                .completionRate(completionRate)
                .averageScore(averageScore)
                .courseCoverage(courseCoverage)
                .activeFacilitators(activeFacilitators)
                .totalCohorts(totalCohorts)
                .pendingAccessRequests((int) pendingAccessRequests)
                .overallSurveyResponseRate(overallSurveyResponseRate)
                .build();
    }

    /**
     * Calculates participant growth percentage (current month vs previous month).
     */
    private BigDecimal calculateParticipantGrowth(String partnerId) {
        LocalDate now = LocalDate.now();
        LocalDate currentMonthStart = now.withDayOfMonth(1);
        LocalDate previousMonthStart = currentMonthStart.minusMonths(1);
        LocalDate previousMonthEnd = currentMonthStart.minusDays(1);
        
        // Count participants created in current month
        long currentMonthCount = participantRepository.findAll().stream()
                .filter(p -> p.getPartner() != null && p.getPartner().getPartnerId().equals(partnerId))
                .filter(p -> {
                    if (p.getCreatedAt() == null) return false;
                    LocalDate createdDate = p.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                    return !createdDate.isBefore(currentMonthStart) && !createdDate.isAfter(now);
                })
                .count();
        
        // Count participants created in previous month
        long previousMonthCount = participantRepository.findAll().stream()
                .filter(p -> p.getPartner() != null && p.getPartner().getPartnerId().equals(partnerId))
                .filter(p -> {
                    if (p.getCreatedAt() == null) return false;
                    LocalDate createdDate = p.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                    return !createdDate.isBefore(previousMonthStart) && !createdDate.isAfter(previousMonthEnd);
                })
                .count();
        
        if (previousMonthCount == 0) {
            return currentMonthCount > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        }
        
        BigDecimal growth = BigDecimal.valueOf(currentMonthCount - previousMonthCount)
                .divide(BigDecimal.valueOf(previousMonthCount), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        
        return growth.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates completion rate (completed enrollments / total enrollments).
     */
    private BigDecimal calculateCompletionRate(String partnerId, UUID cohortIdFilter) {
        List<Enrollment> enrollments = cohortIdFilter != null
                ? enrollmentRepository.findByCohortId(cohortIdFilter)
                : enrollmentRepository.findByParticipantPartnerPartnerId(partnerId);
        
        if (enrollments.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        long completedCount = enrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.COMPLETED)
                .count();
        
        return BigDecimal.valueOf(completedCount)
                .divide(BigDecimal.valueOf(enrollments.size()), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates average score across all assessments.
     */
    private BigDecimal calculateAverageScore(String partnerId, UUID cohortIdFilter, UUID facilitatorIdFilter) {
        List<Score> scores = scoreRepository.findByEnrollmentParticipantPartnerPartnerId(partnerId);
        
        if (cohortIdFilter != null) {
            scores = scores.stream()
                    .filter(s -> s.getEnrollment().getCohort().getId().equals(cohortIdFilter))
                    .collect(Collectors.toList());
        }
        
        if (facilitatorIdFilter != null) {
            scores = scores.stream()
                    .filter(s -> s.getRecordedBy().getId().equals(facilitatorIdFilter))
                    .collect(Collectors.toList());
        }
        
        if (scores.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal sum = scores.stream()
                .map(Score::getScoreValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return sum.divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates course coverage (e.g., "8/8").
     */
    private String calculateCourseCoverage(String partnerId, UUID cohortIdFilter) {
        List<Program> programs = programRepository.findByPartnerPartnerId(partnerId);
        
        if (programs.isEmpty()) {
            return "0/0";
        }
        
        int totalModules = 0;
        int modulesWithScores = 0;
        
        for (Program program : programs) {
            List<TrainingModule> modules = trainingModuleRepository.findByProgramId(program.getId());
            totalModules += modules.size();
            
            for (TrainingModule module : modules) {
                List<Score> scores = scoreRepository.findAll().stream()
                        .filter(s -> s.getModule().getId().equals(module.getId()))
                        .filter(s -> {
                            if (cohortIdFilter != null) {
                                return s.getEnrollment().getCohort().getId().equals(cohortIdFilter);
                            }
                            return s.getEnrollment().getParticipant().getPartner().getPartnerId().equals(partnerId);
                        })
                        .collect(Collectors.toList());
                
                if (!scores.isEmpty()) {
                    modulesWithScores++;
                }
            }
        }
        
        return modulesWithScores + "/" + totalModules;
    }

    /**
     * Counts active facilitators for the partner.
     */
    private int countActiveFacilitators(String partnerId, UUID facilitatorIdFilter) {
        List<User> facilitators = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.FACILITATOR)
                .filter(u -> u.getPartner() != null && u.getPartner().getPartnerId().equals(partnerId))
                .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
                .filter(u -> facilitatorIdFilter == null || u.getId().equals(facilitatorIdFilter))
                .collect(Collectors.toList());
        
        return facilitators.size();
    }

    /**
     * Calculates overall survey response rate.
     */
    private BigDecimal calculateSurveyResponseRate(String partnerId, UUID cohortIdFilter) {
        long totalResponses = surveyResponseRepository.countByParticipantPartnerPartnerId(partnerId);
        long submittedResponses = surveyResponseRepository.countByParticipantPartnerPartnerIdAndSubmitted(partnerId);
        
        if (totalResponses == 0) {
            return BigDecimal.ZERO;
        }
        
        return BigDecimal.valueOf(submittedResponses)
                .divide(BigDecimal.valueOf(totalResponses), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates monthly progress data for bar chart.
     */
    private List<MonthlyDataDTO> calculateMonthlyProgress(String partnerId, LocalDate startDate, LocalDate endDate) {
        List<MonthlyDataDTO> monthlyData = new ArrayList<>();
        
        LocalDate now = LocalDate.now();
        LocalDate start = startDate != null ? startDate : now.minusMonths(11);
        LocalDate end = endDate != null ? endDate : now;
        
        LocalDate current = start.withDayOfMonth(1);
        
        while (!current.isAfter(end)) {
            LocalDate monthStart = current;
            LocalDate monthEnd = current.withDayOfMonth(current.lengthOfMonth());
            
            // Count enrollments created in this month
            long enrollmentsInMonth = enrollmentRepository.findByParticipantPartnerPartnerId(partnerId).stream()
                    .filter(e -> {
                        if (e.getCreatedAt() == null) return false;
                        LocalDate createdDate = e.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                        return !createdDate.isBefore(monthStart) && !createdDate.isAfter(monthEnd);
                    })
                    .count();
            
            String monthName = current.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            monthlyData.add(MonthlyDataDTO.builder()
                    .monthName(monthName)
                    .progressValue(BigDecimal.valueOf(enrollmentsInMonth))
                    .build());
            
            current = current.plusMonths(1);
        }
        
        return monthlyData;
    }

    /**
     * Calculates program distribution (trainingCompleted, inProgress, notStarted).
     */
    private OutcomeDistributionDTO calculateProgramDistribution(String partnerId, UUID cohortIdFilter) {
        List<Enrollment> enrollments = cohortIdFilter != null
                ? enrollmentRepository.findByCohortId(cohortIdFilter)
                : enrollmentRepository.findByParticipantPartnerPartnerId(partnerId);
        
        if (enrollments.isEmpty()) {
            return OutcomeDistributionDTO.builder()
                    .trainingCompleted(BigDecimal.ZERO)
                    .inProgress(BigDecimal.ZERO)
                    .notStarted(BigDecimal.ZERO)
                    .build();
        }
        
        long completed = enrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.COMPLETED)
                .count();
        
        long inProgress = enrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ENROLLED)
                .count();
        
        long notStarted = enrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.WITHDRAWN ||
                           e.getStatus() == EnrollmentStatus.DROPPED_OUT)
                .count();
        
        long total = enrollments.size();
        
        return OutcomeDistributionDTO.builder()
                .trainingCompleted(BigDecimal.valueOf(completed)
                        .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP))
                .inProgress(BigDecimal.valueOf(inProgress)
                        .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP))
                .notStarted(BigDecimal.valueOf(notStarted)
                        .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP))
                .build();
    }

    /**
     * Calculates facilitator performance rankings.
     */
    private List<FacilitatorRankDTO> calculateFacilitatorPerformance(
            String partnerId, UUID facilitatorIdFilter, UUID cohortIdFilter) {
        
        List<User> facilitators = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.FACILITATOR)
                .filter(u -> u.getPartner() != null && u.getPartner().getPartnerId().equals(partnerId))
                .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
                .filter(u -> facilitatorIdFilter == null || u.getId().equals(facilitatorIdFilter))
                .collect(Collectors.toList());
        
        List<FacilitatorRankDTO> rankings = new ArrayList<>();
        
        for (User facilitator : facilitators) {
            // Count participants assigned to this facilitator (through enrollments in their cohorts)
            List<Cohort> facilitatorCohorts = cohortRepository.findByCenterIdAndStatus(
                    facilitator.getCenter().getId(), CohortStatus.ACTIVE);
            
            long participantCount = facilitatorCohorts.stream()
                    .mapToLong(c -> {
                        if (cohortIdFilter != null && !c.getId().equals(cohortIdFilter)) {
                            return 0;
                        }
                        return enrollmentRepository.findByCohortId(c.getId()).size();
                    })
                    .sum();
            
            // Calculate rating based on average score (simplified - can be enhanced)
            List<Score> facilitatorScores = scoreRepository.findAll().stream()
                    .filter(s -> s.getRecordedBy().getId().equals(facilitator.getId()))
                    .filter(s -> {
                        if (cohortIdFilter != null) {
                            return s.getEnrollment().getCohort().getId().equals(cohortIdFilter);
                        }
                        return s.getEnrollment().getParticipant().getPartner().getPartnerId().equals(partnerId);
                    })
                    .collect(Collectors.toList());
            
            BigDecimal rating = BigDecimal.valueOf(4.5); // Default rating
            if (!facilitatorScores.isEmpty()) {
                BigDecimal avgScore = facilitatorScores.stream()
                        .map(Score::getScoreValue)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(facilitatorScores.size()), 2, RoundingMode.HALF_UP);
                
                // Convert score (0-100) to rating (0-5)
                rating = avgScore.divide(BigDecimal.valueOf(20), 2, RoundingMode.HALF_UP);
                if (rating.compareTo(BigDecimal.valueOf(5)) > 0) {
                    rating = BigDecimal.valueOf(5);
                }
            }
            
            rankings.add(FacilitatorRankDTO.builder()
                    .name(facilitator.getFirstName() + " " + facilitator.getLastName())
                    .participantCount((int) participantCount)
                    .rating(rating)
                    .build());
        }
        
        // Sort by participant count descending, then by rating descending
        rankings.sort((a, b) -> {
            int countCompare = Integer.compare(b.getParticipantCount(), a.getParticipantCount());
            if (countCompare != 0) return countCompare;
            return b.getRating().compareTo(a.getRating());
        });
        
        // Return top 10
        return rankings.stream().limit(10).collect(Collectors.toList());
    }

    /**
     * Calculates cohort status with completion percentages.
     */
    private List<CohortStatusDTO> calculateCohortStatus(String partnerId, UUID cohortIdFilter) {
        List<Cohort> cohorts = cohortIdFilter != null
                ? Collections.singletonList(cohortRepository.findById(cohortIdFilter).orElse(null))
                : cohortRepository.findByCenterPartnerPartnerId(partnerId);
        
        cohorts = cohorts.stream().filter(Objects::nonNull).collect(Collectors.toList());
        
        List<CohortStatusDTO> statusList = new ArrayList<>();
        
        for (Cohort cohort : cohorts) {
            List<Enrollment> enrollments = enrollmentRepository.findByCohortId(cohort.getId());
            
            int completionPercentage = 0;
            if (!enrollments.isEmpty()) {
                long completed = enrollments.stream()
                        .filter(e -> e.getStatus() == EnrollmentStatus.COMPLETED)
                        .count();
                completionPercentage = (int) (completed * 100 / enrollments.size());
            }
            
            statusList.add(CohortStatusDTO.builder()
                    .cohortId(cohort.getId())
                    .cohortName(cohort.getCohortName())
                    .status(cohort.getStatus())
                    .completionPercentage(completionPercentage)
                    .build());
        }
        
        // Sort by status (ACTIVE first), then by completion percentage descending
        statusList.sort((a, b) -> {
            if (a.getStatus() != b.getStatus()) {
                return a.getStatus() == CohortStatus.ACTIVE ? -1 : 1;
            }
            return Integer.compare(b.getCompletionPercentage(), a.getCompletionPercentage());
        });
        
        return statusList;
    }

    /**
     * Calculates course enrollment metrics.
     */
    private List<CourseMetricDTO> calculateCourseEnrollment(String partnerId, UUID cohortIdFilter) {
        List<Program> programs = programRepository.findByPartnerPartnerId(partnerId);
        
        List<CourseMetricDTO> courseMetrics = new ArrayList<>();
        
        for (Program program : programs) {
            // Count enrollments for this program
            List<Enrollment> programEnrollments = enrollmentRepository.findByParticipantPartnerPartnerId(partnerId)
                    .stream()
                    .filter(e -> {
                        if (cohortIdFilter != null) {
                            return e.getCohort().getId().equals(cohortIdFilter);
                        }
                        return e.getCohort().getProgram().getId().equals(program.getId());
                    })
                    .collect(Collectors.toList());
            
            // Count unique facilitators assigned to cohorts in this program
            Set<UUID> facilitatorIds = new HashSet<>();
            List<Cohort> programCohorts = cohortRepository.findByCenterPartnerPartnerId(partnerId).stream()
                    .filter(c -> c.getProgram().getId().equals(program.getId()))
                    .filter(c -> cohortIdFilter == null || c.getId().equals(cohortIdFilter))
                    .collect(Collectors.toList());
            
            for (Cohort cohort : programCohorts) {
                List<User> facilitators = userRepository.findAll().stream()
                        .filter(u -> u.getRole() == Role.FACILITATOR)
                        .filter(u -> u.getCenter() != null && u.getCenter().getId().equals(cohort.getCenter().getId()))
                        .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
                        .collect(Collectors.toList());
                
                facilitators.forEach(f -> facilitatorIds.add(f.getId()));
            }
            
            courseMetrics.add(CourseMetricDTO.builder()
                    .courseCategory(program.getProgramName())
                    .enrolledCount(programEnrollments.size())
                    .assignedFacilitators(facilitatorIds.size())
                    .build());
        }
        
        // Sort by enrolled count descending
        courseMetrics.sort((a, b) -> Integer.compare(b.getEnrolledCount(), a.getEnrolledCount()));
        
        return courseMetrics;
    }
}
