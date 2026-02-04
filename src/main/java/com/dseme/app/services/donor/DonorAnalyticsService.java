package com.dseme.app.services.donor;

import com.dseme.app.dtos.donor.*;
import com.dseme.app.enums.EnrollmentStatus;
import com.dseme.app.enums.EmploymentStatus;
import com.dseme.app.enums.InternshipStatus;
import com.dseme.app.enums.SurveyType;
import com.dseme.app.models.Center;
import com.dseme.app.models.Cohort;
import com.dseme.app.models.Enrollment;
import com.dseme.app.models.EmploymentOutcome;
import com.dseme.app.models.Internship;
import com.dseme.app.models.Participant;
import com.dseme.app.models.Partner;
import com.dseme.app.models.Program;
import com.dseme.app.models.Survey;
import com.dseme.app.models.SurveyResponse;
import com.dseme.app.enums.QuestionType;
import com.dseme.app.models.SurveyAnswer;
import com.dseme.app.repositories.CenterRepository;
import com.dseme.app.repositories.CohortRepository;
import com.dseme.app.repositories.EnrollmentRepository;
import com.dseme.app.repositories.EmploymentOutcomeRepository;
import com.dseme.app.repositories.InternshipRepository;
import com.dseme.app.repositories.ParticipantRepository;
import com.dseme.app.repositories.PartnerRepository;
import com.dseme.app.repositories.ProgramRepository;
import com.dseme.app.repositories.SurveyAnswerRepository;
import com.dseme.app.repositories.SurveyRepository;
import com.dseme.app.repositories.SurveyResponseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for DONOR portfolio-level analytics.
 * 
 * Provides aggregated analytics across all partners:
 * - Enrollment KPIs
 * - Completion and dropout metrics
 * 
 * All data is aggregated - no participant-level data is exposed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DonorAnalyticsService {

    private final EnrollmentRepository enrollmentRepository;
    private final PartnerRepository partnerRepository;
    private final ProgramRepository programRepository;
    private final EmploymentOutcomeRepository employmentOutcomeRepository;
    private final InternshipRepository internshipRepository;
    private final SurveyRepository surveyRepository;
    private final SurveyResponseRepository surveyResponseRepository;
    private final ParticipantRepository participantRepository;
    private final CenterRepository centerRepository;
    private final CohortRepository cohortRepository;
    private final SurveyAnswerRepository surveyAnswerRepository;

    /**
     * Gets aggregated enrollment KPIs across all partners.
     * 
     * Includes:
     * - Total enrollments
     * - Enrollment growth over time (monthly)
     * - Enrollment breakdown by partner
     * - Enrollment breakdown by program
     * 
     * @param context DONOR context
     * @return Enrollment analytics DTO
     */
    public EnrollmentAnalyticsDTO getEnrollmentAnalytics(DonorContext context) {
        // Get all enrollments (portfolio-wide)
        // Note: For large datasets, consider using database-level aggregation queries
        List<Enrollment> allEnrollments = enrollmentRepository.findAll();

        // Handle empty dataset
        if (allEnrollments.isEmpty()) {
            return EnrollmentAnalyticsDTO.builder()
                    .totalEnrollments(0L)
                    .enrollmentGrowth(new ArrayList<>())
                    .enrollmentByPartner(new ArrayList<>())
                    .enrollmentByProgram(new ArrayList<>())
                    .build();
        }

        // Calculate total enrollments
        long totalEnrollments = allEnrollments.size();

        // Calculate enrollment growth over time (monthly)
        List<EnrollmentGrowthDTO> enrollmentGrowth = calculateEnrollmentGrowth(allEnrollments);

        // Calculate enrollment by partner
        List<EnrollmentByPartnerDTO> enrollmentByPartner = calculateEnrollmentByPartner(allEnrollments, totalEnrollments);

        // Calculate enrollment by program
        List<EnrollmentByProgramDTO> enrollmentByProgram = calculateEnrollmentByProgram(allEnrollments, totalEnrollments);

        return EnrollmentAnalyticsDTO.builder()
                .totalEnrollments(totalEnrollments)
                .enrollmentGrowth(enrollmentGrowth)
                .enrollmentByPartner(enrollmentByPartner)
                .enrollmentByProgram(enrollmentByProgram)
                .build();
    }

    /**
     * Gets completion and dropout metrics across all partners.
     * 
     * Includes:
     * - Completion rate
     * - Dropout rate
     * - Dropout reasons (grouped)
     * 
     * @param context DONOR context
     * @return Completion metrics DTO
     */
    public CompletionMetricsDTO getCompletionMetrics(DonorContext context) {
        // Get all enrollments (portfolio-wide)
        // Note: For large datasets, consider using database-level aggregation queries
        List<Enrollment> allEnrollments = enrollmentRepository.findAll();

        // Handle empty dataset
        if (allEnrollments.isEmpty()) {
            return CompletionMetricsDTO.builder()
                    .completionRate(BigDecimal.ZERO)
                    .dropoutRate(BigDecimal.ZERO)
                    .totalCompleted(0L)
                    .totalDroppedOut(0L)
                    .totalActive(0L)
                    .totalEnrollments(0L)
                    .dropoutReasons(new ArrayList<>())
                    .build();
        }

        // Calculate totals by status
        long totalCompleted = allEnrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.COMPLETED)
                .count();

        long totalDroppedOut = allEnrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.DROPPED_OUT)
                .count();

        long totalActive = allEnrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE || e.getStatus() == EnrollmentStatus.ENROLLED)
                .count();

        long totalEnrollments = allEnrollments.size();

        // Calculate rates
        BigDecimal completionRate = totalEnrollments > 0 ?
                BigDecimal.valueOf(totalCompleted)
                        .divide(BigDecimal.valueOf(totalEnrollments), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        BigDecimal dropoutRate = totalEnrollments > 0 ?
                BigDecimal.valueOf(totalDroppedOut)
                        .divide(BigDecimal.valueOf(totalEnrollments), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        // Group dropout reasons
        List<DropoutReasonGroupDTO> dropoutReasons = groupDropoutReasons(allEnrollments, totalDroppedOut);

        return CompletionMetricsDTO.builder()
                .completionRate(completionRate)
                .dropoutRate(dropoutRate)
                .totalCompleted(totalCompleted)
                .totalDroppedOut(totalDroppedOut)
                .totalActive(totalActive)
                .totalEnrollments(totalEnrollments)
                .dropoutReasons(dropoutReasons)
                .build();
    }

    /**
     * Calculates enrollment growth over time (monthly breakdown).
     */
    private List<EnrollmentGrowthDTO> calculateEnrollmentGrowth(List<Enrollment> enrollments) {
        // Filter out enrollments with null enrollment dates
        List<Enrollment> validEnrollments = enrollments.stream()
                .filter(e -> e.getEnrollmentDate() != null)
                .collect(Collectors.toList());

        if (validEnrollments.isEmpty()) {
            return new ArrayList<>();
        }

        // Group enrollments by year-month
        Map<String, Long> enrollmentsByMonth = validEnrollments.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getEnrollmentDate().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                        Collectors.counting()
                ));

        // Convert to DTOs and sort by period
        List<EnrollmentGrowthDTO> growthList = new ArrayList<>();
        List<String> sortedPeriods = enrollmentsByMonth.keySet().stream()
                .sorted()
                .collect(Collectors.toList());

        for (int i = 0; i < sortedPeriods.size(); i++) {
            String period = sortedPeriods.get(i);
            Long count = enrollmentsByMonth.get(period);

            // Calculate growth percentage
            BigDecimal growthPercentage = BigDecimal.ZERO;
            if (i > 0) {
                String previousPeriod = sortedPeriods.get(i - 1);
                Long previousCount = enrollmentsByMonth.get(previousPeriod);
                if (previousCount > 0) {
                    growthPercentage = BigDecimal.valueOf(count - previousCount)
                            .divide(BigDecimal.valueOf(previousCount), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(2, RoundingMode.HALF_UP);
                } else if (count > 0) {
                    growthPercentage = BigDecimal.valueOf(100);
                }
            }

            growthList.add(EnrollmentGrowthDTO.builder()
                    .period(period)
                    .enrollments(count)
                    .growthPercentage(growthPercentage)
                    .build());
        }

        return growthList;
    }

    /**
     * Calculates enrollment breakdown by partner.
     */
    private List<EnrollmentByPartnerDTO> calculateEnrollmentByPartner(
            List<Enrollment> enrollments,
            long totalEnrollments
    ) {
        // Group enrollments by partner
        Map<String, Long> enrollmentsByPartner = enrollments.stream()
                .filter(e -> e.getParticipant() != null && e.getParticipant().getPartner() != null)
                .collect(Collectors.groupingBy(
                        e -> e.getParticipant().getPartner().getPartnerId(),
                        Collectors.counting()
                ));

        // Get all partners for names
        Map<String, Partner> partnersMap = partnerRepository.findAll().stream()
                .collect(Collectors.toMap(Partner::getPartnerId, p -> p));

        // Convert to DTOs
        List<EnrollmentByPartnerDTO> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : enrollmentsByPartner.entrySet()) {
            String partnerId = entry.getKey();
            Long count = entry.getValue();
            Partner partner = partnersMap.get(partnerId);

            BigDecimal percentage = totalEnrollments > 0 ?
                    BigDecimal.valueOf(count)
                            .divide(BigDecimal.valueOf(totalEnrollments), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(2, RoundingMode.HALF_UP) :
                    BigDecimal.ZERO;

            result.add(EnrollmentByPartnerDTO.builder()
                    .partnerId(partnerId)
                    .partnerName(partner != null ? partner.getPartnerName() : "Unknown")
                    .totalEnrollments(count)
                    .percentage(percentage)
                    .build());
        }

        // Sort by total enrollments descending
        result.sort((a, b) -> Long.compare(b.getTotalEnrollments(), a.getTotalEnrollments()));

        return result;
    }

    /**
     * Calculates enrollment breakdown by program.
     */
    private List<EnrollmentByProgramDTO> calculateEnrollmentByProgram(
            List<Enrollment> enrollments,
            long totalEnrollments
    ) {
        // Group enrollments by program (through cohort)
        Map<UUID, Long> enrollmentsByProgram = enrollments.stream()
                .filter(e -> e.getCohort() != null && e.getCohort().getProgram() != null)
                .collect(Collectors.groupingBy(
                        e -> e.getCohort().getProgram().getId(),
                        Collectors.counting()
                ));

        // Get all programs for names and partner info
        Map<UUID, Program> programsMap = programRepository.findAll().stream()
                .collect(Collectors.toMap(Program::getId, p -> p));

        // Convert to DTOs
        List<EnrollmentByProgramDTO> result = new ArrayList<>();
        for (Map.Entry<UUID, Long> entry : enrollmentsByProgram.entrySet()) {
            UUID programId = entry.getKey();
            Long count = entry.getValue();
            Program program = programsMap.get(programId);

            if (program == null) continue;

            BigDecimal percentage = totalEnrollments > 0 ?
                    BigDecimal.valueOf(count)
                            .divide(BigDecimal.valueOf(totalEnrollments), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(2, RoundingMode.HALF_UP) :
                    BigDecimal.ZERO;

            result.add(EnrollmentByProgramDTO.builder()
                    .programId(programId)
                    .programName(program.getProgramName())
                    .partnerId(program.getPartner() != null ? program.getPartner().getPartnerId() : null)
                    .partnerName(program.getPartner() != null ? program.getPartner().getPartnerName() : "Unknown")
                    .totalEnrollments(count)
                    .percentage(percentage)
                    .build());
        }

        // Sort by total enrollments descending
        result.sort((a, b) -> Long.compare(b.getTotalEnrollments(), a.getTotalEnrollments()));

        return result;
    }

    /**
     * Groups dropout reasons and calculates counts/percentages.
     */
    private List<DropoutReasonGroupDTO> groupDropoutReasons(
            List<Enrollment> enrollments,
            long totalDroppedOut
    ) {
        // Filter only dropped out enrollments
        List<Enrollment> droppedOut = enrollments.stream()
                .filter(e -> e != null && e.getStatus() == EnrollmentStatus.DROPPED_OUT)
                .collect(Collectors.toList());

        if (droppedOut.isEmpty()) {
            return new ArrayList<>();
        }

        // Group by dropout reason
        Map<String, Long> reasonsMap = droppedOut.stream()
                .collect(Collectors.groupingBy(
                        e -> {
                            String reason = e.getDropoutReason();
                            return (reason != null && !reason.trim().isEmpty()) ? reason.trim() : "Not specified";
                        },
                        Collectors.counting()
                ));

        // Convert to DTOs
        List<DropoutReasonGroupDTO> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : reasonsMap.entrySet()) {
            String reason = entry.getKey();
            Long count = entry.getValue();

            BigDecimal percentage = totalDroppedOut > 0 ?
                    BigDecimal.valueOf(count)
                            .divide(BigDecimal.valueOf(totalDroppedOut), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(2, RoundingMode.HALF_UP) :
                    BigDecimal.ZERO;

            result.add(DropoutReasonGroupDTO.builder()
                    .reason(reason)
                    .count(count)
                    .percentage(percentage)
                    .build());
        }

        // Sort by count descending
        result.sort((a, b) -> Long.compare(b.getCount(), a.getCount()));

        return result;
    }

    /**
     * Gets portfolio-level employment outcomes analytics.
     * 
     * Includes:
     * - Employment rate by partner
     * - Employment rate by cohort
     * - Internship-to-employment conversion
     * 
     * @param context DONOR context
     * @return Employment analytics DTO
     */
    public EmploymentAnalyticsDTO getEmploymentAnalytics(DonorContext context) {
        // Get all completed enrollments (portfolio-wide)
        List<Enrollment> completedEnrollments = enrollmentRepository.findAll().stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.COMPLETED)
                .collect(Collectors.toList());

        if (completedEnrollments.isEmpty()) {
            return EmploymentAnalyticsDTO.builder()
                    .overallEmploymentRate(BigDecimal.ZERO)
                    .totalCompletedEnrollments(0L)
                    .totalEmployed(0L)
                    .employmentByPartner(new ArrayList<>())
                    .employmentByCohort(new ArrayList<>())
                    .internshipConversion(InternshipConversionDTO.builder()
                            .totalCompletedInternships(0L)
                            .internshipsConvertedToEmployment(0L)
                            .conversionRate(BigDecimal.ZERO)
                            .build())
                    .build();
        }

        // Get all employment outcomes
        List<EmploymentOutcome> allEmploymentOutcomes = employmentOutcomeRepository.findAll();
        
        // Filter employed outcomes (EMPLOYED or SELF_EMPLOYED)
        List<EmploymentOutcome> employedOutcomes = allEmploymentOutcomes.stream()
                .filter(eo -> eo.getEmploymentStatus() == EmploymentStatus.EMPLOYED ||
                             eo.getEmploymentStatus() == EmploymentStatus.SELF_EMPLOYED)
                .collect(Collectors.toList());

        long totalCompleted = completedEnrollments.size();
        long totalEmployed = employedOutcomes.size();

        // Calculate overall employment rate
        BigDecimal overallEmploymentRate = totalCompleted > 0 ?
                BigDecimal.valueOf(totalEmployed)
                        .divide(BigDecimal.valueOf(totalCompleted), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        // Calculate employment by partner
        List<EmploymentByPartnerDTO> employmentByPartner = calculateEmploymentByPartner(
                completedEnrollments, employedOutcomes);

        // Calculate employment by cohort
        List<EmploymentByCohortDTO> employmentByCohort = calculateEmploymentByCohort(
                completedEnrollments, employedOutcomes);

        // Calculate internship-to-employment conversion
        InternshipConversionDTO internshipConversion = calculateInternshipConversion();

        return EmploymentAnalyticsDTO.builder()
                .overallEmploymentRate(overallEmploymentRate)
                .totalCompletedEnrollments(totalCompleted)
                .totalEmployed(totalEmployed)
                .employmentByPartner(employmentByPartner)
                .employmentByCohort(employmentByCohort)
                .internshipConversion(internshipConversion)
                .build();
    }

    /**
     * Gets longitudinal impact tracking (baseline vs endline vs tracer).
     * 
     * Compares survey responses across different survey types.
     * Returns time-series friendly data for charting.
     * 
     * @param context DONOR context
     * @return Longitudinal impact DTO
     */
    public LongitudinalImpactDTO getLongitudinalImpact(DonorContext context) {
        // Get all surveys (portfolio-wide)
        List<Survey> allSurveys = surveyRepository.findAll();

        // Filter surveys by type
        List<Survey> baselineSurveys = allSurveys.stream()
                .filter(s -> s.getSurveyType() == SurveyType.BASELINE)
                .collect(Collectors.toList());

        List<Survey> endlineSurveys = allSurveys.stream()
                .filter(s -> s.getSurveyType() == SurveyType.ENDLINE)
                .collect(Collectors.toList());

        List<Survey> tracerSurveys = allSurveys.stream()
                .filter(s -> s.getSurveyType() == SurveyType.TRACER)
                .collect(Collectors.toList());

        // Get all survey responses
        List<SurveyResponse> allResponses = surveyResponseRepository.findAll();

        // Calculate time-series data
        List<SurveyTimeSeriesDTO> timeSeries = calculateSurveyTimeSeries(
                baselineSurveys, endlineSurveys, tracerSurveys, allResponses);

        // Calculate comparison metrics
        SurveyComparisonDTO comparison = calculateSurveyComparison(
                baselineSurveys, endlineSurveys, tracerSurveys, allResponses);

        return LongitudinalImpactDTO.builder()
                .timeSeries(timeSeries)
                .comparison(comparison)
                .build();
    }

    /**
     * Calculates employment breakdown by partner.
     */
    private List<EmploymentByPartnerDTO> calculateEmploymentByPartner(
            List<Enrollment> completedEnrollments,
            List<EmploymentOutcome> employedOutcomes
    ) {
        // Group completed enrollments by partner
        Map<String, Long> completedByPartner = completedEnrollments.stream()
                .filter(e -> e.getParticipant() != null && e.getParticipant().getPartner() != null)
                .collect(Collectors.groupingBy(
                        e -> e.getParticipant().getPartner().getPartnerId(),
                        Collectors.counting()
                ));

        // Group employed outcomes by partner (through enrollment -> participant)
        Map<String, Long> employedByPartner = employedOutcomes.stream()
                .filter(eo -> eo.getEnrollment() != null &&
                             eo.getEnrollment().getParticipant() != null &&
                             eo.getEnrollment().getParticipant().getPartner() != null)
                .collect(Collectors.groupingBy(
                        eo -> eo.getEnrollment().getParticipant().getPartner().getPartnerId(),
                        Collectors.counting()
                ));

        // Get all partners for names
        Map<String, Partner> partnersMap = partnerRepository.findAll().stream()
                .collect(Collectors.toMap(Partner::getPartnerId, p -> p));

        // Convert to DTOs
        List<EmploymentByPartnerDTO> result = new ArrayList<>();
        Set<String> allPartnerIds = new HashSet<>(completedByPartner.keySet());
        allPartnerIds.addAll(employedByPartner.keySet());

        for (String partnerId : allPartnerIds) {
            Long completed = completedByPartner.getOrDefault(partnerId, 0L);
            Long employed = employedByPartner.getOrDefault(partnerId, 0L);
            Partner partner = partnersMap.get(partnerId);

            BigDecimal employmentRate = completed > 0 ?
                    BigDecimal.valueOf(employed)
                            .divide(BigDecimal.valueOf(completed), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(2, RoundingMode.HALF_UP) :
                    BigDecimal.ZERO;

            result.add(EmploymentByPartnerDTO.builder()
                    .partnerId(partnerId)
                    .partnerName(partner != null ? partner.getPartnerName() : "Unknown")
                    .totalCompletedEnrollments(completed)
                    .totalEmployed(employed)
                    .employmentRate(employmentRate)
                    .build());
        }

        // Sort by employment rate descending
        result.sort((a, b) -> b.getEmploymentRate().compareTo(a.getEmploymentRate()));

        return result;
    }

    /**
     * Calculates employment breakdown by cohort.
     */
    private List<EmploymentByCohortDTO> calculateEmploymentByCohort(
            List<Enrollment> completedEnrollments,
            List<EmploymentOutcome> employedOutcomes
    ) {
        // Group completed enrollments by cohort
        Map<UUID, Long> completedByCohort = completedEnrollments.stream()
                .filter(e -> e.getCohort() != null)
                .collect(Collectors.groupingBy(
                        e -> e.getCohort().getId(),
                        Collectors.counting()
                ));

        // Group employed outcomes by cohort (through enrollment)
        Map<UUID, Long> employedByCohort = employedOutcomes.stream()
                .filter(eo -> eo.getEnrollment() != null && eo.getEnrollment().getCohort() != null)
                .collect(Collectors.groupingBy(
                        eo -> eo.getEnrollment().getCohort().getId(),
                        Collectors.counting()
                ));

        // Get all cohorts for metadata
        Map<UUID, com.dseme.app.models.Cohort> cohortsMap = enrollmentRepository.findAll().stream()
                .filter(e -> e.getCohort() != null)
                .map(Enrollment::getCohort)
                .distinct()
                .collect(Collectors.toMap(
                        com.dseme.app.models.Cohort::getId,
                        c -> c,
                        (c1, c2) -> c1
                ));

        // Convert to DTOs
        List<EmploymentByCohortDTO> result = new ArrayList<>();
        Set<UUID> allCohortIds = new HashSet<>(completedByCohort.keySet());
        allCohortIds.addAll(employedByCohort.keySet());

        for (UUID cohortId : allCohortIds) {
            Long completed = completedByCohort.getOrDefault(cohortId, 0L);
            Long employed = employedByCohort.getOrDefault(cohortId, 0L);
            com.dseme.app.models.Cohort cohort = cohortsMap.get(cohortId);

            if (cohort == null) continue;

            BigDecimal employmentRate = completed > 0 ?
                    BigDecimal.valueOf(employed)
                            .divide(BigDecimal.valueOf(completed), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(2, RoundingMode.HALF_UP) :
                    BigDecimal.ZERO;

            result.add(EmploymentByCohortDTO.builder()
                    .cohortId(cohortId)
                    .cohortName(cohort.getCohortName())
                    .programId(cohort.getProgram() != null ? cohort.getProgram().getId() : null)
                    .programName(cohort.getProgram() != null ? cohort.getProgram().getProgramName() : "Unknown")
                    .partnerId(cohort.getCenter() != null && cohort.getCenter().getPartner() != null ?
                            cohort.getCenter().getPartner().getPartnerId() : null)
                    .partnerName(cohort.getCenter() != null && cohort.getCenter().getPartner() != null ?
                            cohort.getCenter().getPartner().getPartnerName() : "Unknown")
                    .cohortStartDate(cohort.getStartDate())
                    .cohortEndDate(cohort.getEndDate())
                    .totalCompletedEnrollments(completed)
                    .totalEmployed(employed)
                    .employmentRate(employmentRate)
                    .build());
        }

        // Sort by cohort end date descending (most recent first)
        result.sort((a, b) -> {
            if (a.getCohortEndDate() == null && b.getCohortEndDate() == null) return 0;
            if (a.getCohortEndDate() == null) return 1;
            if (b.getCohortEndDate() == null) return -1;
            return b.getCohortEndDate().compareTo(a.getCohortEndDate());
        });

        return result;
    }

    /**
     * Calculates internship-to-employment conversion metrics.
     */
    private InternshipConversionDTO calculateInternshipConversion() {
        // Get all completed internships
        List<Internship> allInternships = internshipRepository.findAll();
        List<Internship> completedInternships = allInternships.stream()
                .filter(i -> i.getStatus() == InternshipStatus.COMPLETED)
                .collect(Collectors.toList());

        long totalCompletedInternships = completedInternships.size();

        if (totalCompletedInternships == 0) {
            return InternshipConversionDTO.builder()
                    .totalCompletedInternships(0L)
                    .internshipsConvertedToEmployment(0L)
                    .conversionRate(BigDecimal.ZERO)
                    .build();
        }

        // Get all employment outcomes
        List<EmploymentOutcome> allEmploymentOutcomes = employmentOutcomeRepository.findAll();

        // Count how many completed internships led to employment
        // An internship converts to employment if:
        // 1. The participant has a completed internship
        // 2. The participant has an employment outcome with EMPLOYED or SELF_EMPLOYED status
        // 3. The employment outcome references the internship OR is for the same enrollment
        long internshipsConvertedToEmployment = completedInternships.stream()
                .filter(internship -> {
                    UUID enrollmentId = internship.getEnrollment().getId();
                    return allEmploymentOutcomes.stream()
                            .anyMatch(eo -> eo.getEnrollment().getId().equals(enrollmentId) &&
                                    (eo.getEmploymentStatus() == EmploymentStatus.EMPLOYED ||
                                     eo.getEmploymentStatus() == EmploymentStatus.SELF_EMPLOYED) &&
                                    (eo.getInternship() != null && eo.getInternship().getId().equals(internship.getId()) ||
                                     eo.getInternship() == null)); // Also count if no internship link but same enrollment
                })
                .count();

        BigDecimal conversionRate = totalCompletedInternships > 0 ?
                BigDecimal.valueOf(internshipsConvertedToEmployment)
                        .divide(BigDecimal.valueOf(totalCompletedInternships), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        return InternshipConversionDTO.builder()
                .totalCompletedInternships(totalCompletedInternships)
                .internshipsConvertedToEmployment(internshipsConvertedToEmployment)
                .conversionRate(conversionRate)
                .build();
    }

    /**
     * Calculates survey time-series data for charting.
     */
    private List<SurveyTimeSeriesDTO> calculateSurveyTimeSeries(
            List<Survey> baselineSurveys,
            List<Survey> endlineSurveys,
            List<Survey> tracerSurveys,
            List<SurveyResponse> allResponses
    ) {
        List<SurveyTimeSeriesDTO> timeSeries = new ArrayList<>();

        // Process baseline surveys
        for (Survey survey : baselineSurveys) {
            SurveyMetricsDTO metrics = calculateSurveyMetrics(survey, allResponses);
            timeSeries.add(SurveyTimeSeriesDTO.builder()
                    .surveyType("BASELINE")
                    .surveyDate(survey.getStartDate() != null ? survey.getStartDate() : 
                               survey.getCreatedAt() != null ? 
                               survey.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate() : 
                               LocalDate.now())
                    .totalSurveys(1L)
                    .totalResponses(metrics.getTotalResponses())
                    .responseRate(metrics.getResponseRate())
                    .averageResponseTime(metrics.getAverageResponseTime())
                    .build());
        }

        // Process endline surveys
        for (Survey survey : endlineSurveys) {
            SurveyMetricsDTO metrics = calculateSurveyMetrics(survey, allResponses);
            timeSeries.add(SurveyTimeSeriesDTO.builder()
                    .surveyType("ENDLINE")
                    .surveyDate(survey.getStartDate() != null ? survey.getStartDate() : 
                               survey.getCreatedAt() != null ? 
                               survey.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate() : 
                               LocalDate.now())
                    .totalSurveys(1L)
                    .totalResponses(metrics.getTotalResponses())
                    .responseRate(metrics.getResponseRate())
                    .averageResponseTime(metrics.getAverageResponseTime())
                    .build());
        }

        // Process tracer surveys
        for (Survey survey : tracerSurveys) {
            SurveyMetricsDTO metrics = calculateSurveyMetrics(survey, allResponses);
            timeSeries.add(SurveyTimeSeriesDTO.builder()
                    .surveyType("TRACER")
                    .surveyDate(survey.getStartDate() != null ? survey.getStartDate() : 
                               survey.getCreatedAt() != null ? 
                               survey.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate() : 
                               LocalDate.now())
                    .totalSurveys(1L)
                    .totalResponses(metrics.getTotalResponses())
                    .responseRate(metrics.getResponseRate())
                    .averageResponseTime(metrics.getAverageResponseTime())
                    .build());
        }

        // Sort by date
        timeSeries.sort((a, b) -> a.getSurveyDate().compareTo(b.getSurveyDate()));

        return timeSeries;
    }

    /**
     * Calculates survey comparison metrics (baseline vs endline vs tracer).
     */
    private SurveyComparisonDTO calculateSurveyComparison(
            List<Survey> baselineSurveys,
            List<Survey> endlineSurveys,
            List<Survey> tracerSurveys,
            List<SurveyResponse> allResponses
    ) {
        // Calculate metrics for each survey type
        SurveyMetricsDTO baselineMetrics = calculateAggregatedSurveyMetrics(baselineSurveys, allResponses);
        SurveyMetricsDTO endlineMetrics = calculateAggregatedSurveyMetrics(endlineSurveys, allResponses);
        SurveyMetricsDTO tracerMetrics = calculateAggregatedSurveyMetrics(tracerSurveys, allResponses);

        // Calculate changes
        BigDecimal baselineToEndlineChange = BigDecimal.ZERO;
        if (baselineMetrics.getResponseRate() != null && endlineMetrics.getResponseRate() != null) {
            baselineToEndlineChange = endlineMetrics.getResponseRate()
                    .subtract(baselineMetrics.getResponseRate())
                    .setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal endlineToTracerChange = BigDecimal.ZERO;
        if (endlineMetrics.getResponseRate() != null && tracerMetrics.getResponseRate() != null) {
            endlineToTracerChange = tracerMetrics.getResponseRate()
                    .subtract(endlineMetrics.getResponseRate())
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return SurveyComparisonDTO.builder()
                .baseline(baselineMetrics)
                .endline(endlineMetrics)
                .tracer(tracerMetrics)
                .baselineToEndlineChange(baselineToEndlineChange)
                .endlineToTracerChange(endlineToTracerChange)
                .build();
    }

    /**
     * Calculates metrics for a single survey.
     */
    private SurveyMetricsDTO calculateSurveyMetrics(Survey survey, List<SurveyResponse> allResponses) {
        // Filter responses for this survey
        List<SurveyResponse> surveyResponses = allResponses.stream()
                .filter(r -> r.getSurvey() != null && r.getSurvey().getId().equals(survey.getId()))
                .collect(Collectors.toList());

        long totalSurveys = 1L; // Single survey
        long totalResponses = surveyResponses.size(); // All responses (including pending)
        long submittedResponses = surveyResponses.stream()
                .filter(r -> r.getSubmittedAt() != null)
                .count();

        // Response rate = submitted responses / total responses (if any responses exist)
        BigDecimal responseRate = totalResponses > 0 ?
                BigDecimal.valueOf(submittedResponses)
                        .divide(BigDecimal.valueOf(totalResponses), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        // Calculate average response time (days between survey creation and submission)
        BigDecimal averageResponseTime = BigDecimal.ZERO;
        List<Long> responseTimes = surveyResponses.stream()
                .filter(r -> r.getSubmittedAt() != null && survey.getCreatedAt() != null)
                .map(r -> {
                    long days = java.time.Duration.between(
                            survey.getCreatedAt(),
                            r.getSubmittedAt()
                    ).toDays();
                    return days;
                })
                .collect(Collectors.toList());

        if (!responseTimes.isEmpty()) {
            double avgDays = responseTimes.stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0.0);
            averageResponseTime = BigDecimal.valueOf(avgDays)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return SurveyMetricsDTO.builder()
                .totalSurveys(totalSurveys)
                .totalResponses(submittedResponses) // Only count submitted responses
                .responseRate(responseRate)
                .averageResponseTime(averageResponseTime)
                .build();
    }

    /**
     * Calculates aggregated metrics for multiple surveys of the same type.
     */
    private SurveyMetricsDTO calculateAggregatedSurveyMetrics(
            List<Survey> surveys,
            List<SurveyResponse> allResponses
    ) {
        if (surveys.isEmpty()) {
            return SurveyMetricsDTO.builder()
                    .totalSurveys(0L)
                    .totalResponses(0L)
                    .responseRate(BigDecimal.ZERO)
                    .averageResponseTime(BigDecimal.ZERO)
                    .build();
        }

        long totalSurveys = surveys.size();
        
        // Count all responses (including pending) and submitted responses
        long totalAllResponses = surveys.stream()
                .mapToLong(survey -> {
                    return allResponses.stream()
                            .filter(r -> r.getSurvey() != null && 
                                       r.getSurvey().getId().equals(survey.getId()))
                            .count();
                })
                .sum();
        
        long totalSubmittedResponses = surveys.stream()
                .mapToLong(survey -> {
                    return allResponses.stream()
                            .filter(r -> r.getSurvey() != null && 
                                       r.getSurvey().getId().equals(survey.getId()) &&
                                       r.getSubmittedAt() != null)
                            .count();
                })
                .sum();

        // Response rate = submitted responses / total responses
        BigDecimal responseRate = totalAllResponses > 0 ?
                BigDecimal.valueOf(totalSubmittedResponses)
                        .divide(BigDecimal.valueOf(totalAllResponses), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        // Calculate average response time across all surveys
        List<Long> allResponseTimes = new ArrayList<>();
        for (Survey survey : surveys) {
            List<SurveyResponse> surveyResponses = allResponses.stream()
                    .filter(r -> r.getSurvey() != null && r.getSurvey().getId().equals(survey.getId()))
                    .collect(Collectors.toList());

            for (SurveyResponse response : surveyResponses) {
                if (response.getSubmittedAt() != null && survey.getCreatedAt() != null) {
                    long days = java.time.Duration.between(
                            survey.getCreatedAt(),
                            response.getSubmittedAt()
                    ).toDays();
                    allResponseTimes.add(days);
                }
            }
        }

        BigDecimal averageResponseTime = BigDecimal.ZERO;
        if (!allResponseTimes.isEmpty()) {
            double avgDays = allResponseTimes.stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0.0);
            averageResponseTime = BigDecimal.valueOf(avgDays)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return SurveyMetricsDTO.builder()
                .totalSurveys(totalSurveys)
                .totalResponses(totalSubmittedResponses) // Only count submitted responses
                .responseRate(responseRate)
                .averageResponseTime(averageResponseTime)
                .build();
    }

    /**
     * Gets demographic and inclusion analytics across all partners.
     * 
     * Includes:
     * - Gender breakdown
     * - Disability status breakdown
     * - Education level breakdown
     * 
     * All data is grouped counts only - no personal identifiers.
     * 
     * @param context DONOR context
     * @return Demographic analytics DTO
     */
    public DemographicAnalyticsDTO getDemographicAnalytics(DonorContext context) {
        // Get all participants (portfolio-wide)
        List<Participant> allParticipants = participantRepository.findAll();

        if (allParticipants.isEmpty()) {
            return DemographicAnalyticsDTO.builder()
                    .totalParticipants(0L)
                    .genderBreakdown(new ArrayList<>())
                    .disabilityBreakdown(new ArrayList<>())
                    .educationBreakdown(new ArrayList<>())
                    .build();
        }

        long totalParticipants = allParticipants.size();

        // Calculate gender breakdown
        List<GenderBreakdownDTO> genderBreakdown = calculateGenderBreakdown(allParticipants, totalParticipants);

        // Calculate disability breakdown
        List<DisabilityBreakdownDTO> disabilityBreakdown = calculateDisabilityBreakdown(allParticipants, totalParticipants);

        // Calculate education breakdown
        List<EducationBreakdownDTO> educationBreakdown = calculateEducationBreakdown(allParticipants, totalParticipants);

        return DemographicAnalyticsDTO.builder()
                .totalParticipants(totalParticipants)
                .genderBreakdown(genderBreakdown)
                .disabilityBreakdown(disabilityBreakdown)
                .educationBreakdown(educationBreakdown)
                .build();
    }

    /**
     * Gets regional analytics across all partners.
     * 
     * Includes:
     * - Breakdown by center
     * - Breakdown by region (aggregated across centers)
     * - Breakdown by country (aggregated across regions)
     * 
     * Cross-partner comparison is allowed.
     * 
     * @param context DONOR context
     * @return Regional analytics DTO
     */
    public RegionalAnalyticsDTO getRegionalAnalytics(DonorContext context) {
        // Get all centers (portfolio-wide)
        List<Center> allCenters = centerRepository.findAll();
        
        // Get all participants
        List<Participant> allParticipants = participantRepository.findAll();
        
        // Get all enrollments
        List<Enrollment> allEnrollments = enrollmentRepository.findAll();
        
        // Get all cohorts
        List<Cohort> allCohorts = cohortRepository.findAll();

        // Calculate center breakdown
        List<CenterAnalyticsDTO> centerBreakdown = calculateCenterBreakdown(
                allCenters, allParticipants, allEnrollments, allCohorts);

        // Calculate region breakdown (aggregated)
        List<RegionAnalyticsDTO> regionBreakdown = calculateRegionBreakdown(
                allCenters, allParticipants, allEnrollments, allCohorts);

        // Calculate country breakdown (aggregated)
        List<CountryAnalyticsDTO> countryBreakdown = calculateCountryBreakdown(
                allCenters, allParticipants, allEnrollments, allCohorts);

        return RegionalAnalyticsDTO.builder()
                .centerBreakdown(centerBreakdown)
                .regionBreakdown(regionBreakdown)
                .countryBreakdown(countryBreakdown)
                .build();
    }

    /**
     * Calculates gender breakdown.
     */
    private List<GenderBreakdownDTO> calculateGenderBreakdown(
            List<Participant> participants,
            long totalParticipants
    ) {
        // Group by gender
        Map<String, Long> genderMap = participants.stream()
                .filter(p -> p.getGender() != null)
                .collect(Collectors.groupingBy(
                        p -> p.getGender().name(),
                        Collectors.counting()
                ));

        List<GenderBreakdownDTO> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : genderMap.entrySet()) {
            String gender = entry.getKey();
            Long count = entry.getValue();

            BigDecimal percentage = totalParticipants > 0 ?
                    BigDecimal.valueOf(count)
                            .divide(BigDecimal.valueOf(totalParticipants), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(2, RoundingMode.HALF_UP) :
                    BigDecimal.ZERO;

            result.add(GenderBreakdownDTO.builder()
                    .gender(gender)
                    .count(count)
                    .percentage(percentage)
                    .build());
        }

        // Sort by count descending
        result.sort((a, b) -> Long.compare(b.getCount(), a.getCount()));

        return result;
    }

    /**
     * Calculates disability status breakdown.
     */
    private List<DisabilityBreakdownDTO> calculateDisabilityBreakdown(
            List<Participant> participants,
            long totalParticipants
    ) {
        // Group by disability status
        Map<String, Long> disabilityMap = participants.stream()
                .filter(p -> p.getDisabilityStatus() != null)
                .collect(Collectors.groupingBy(
                        p -> p.getDisabilityStatus().name(),
                        Collectors.counting()
                ));

        List<DisabilityBreakdownDTO> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : disabilityMap.entrySet()) {
            String disabilityStatus = entry.getKey();
            Long count = entry.getValue();

            BigDecimal percentage = totalParticipants > 0 ?
                    BigDecimal.valueOf(count)
                            .divide(BigDecimal.valueOf(totalParticipants), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(2, RoundingMode.HALF_UP) :
                    BigDecimal.ZERO;

            result.add(DisabilityBreakdownDTO.builder()
                    .disabilityStatus(disabilityStatus)
                    .count(count)
                    .percentage(percentage)
                    .build());
        }

        // Sort by count descending
        result.sort((a, b) -> Long.compare(b.getCount(), a.getCount()));

        return result;
    }

    /**
     * Calculates education level breakdown.
     */
    private List<EducationBreakdownDTO> calculateEducationBreakdown(
            List<Participant> participants,
            long totalParticipants
    ) {
        // Group by education level
        Map<String, Long> educationMap = participants.stream()
                .filter(p -> p.getEducationLevel() != null && !p.getEducationLevel().trim().isEmpty())
                .collect(Collectors.groupingBy(
                        p -> p.getEducationLevel().trim(),
                        Collectors.counting()
                ));

        List<EducationBreakdownDTO> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : educationMap.entrySet()) {
            String educationLevel = entry.getKey();
            Long count = entry.getValue();

            BigDecimal percentage = totalParticipants > 0 ?
                    BigDecimal.valueOf(count)
                            .divide(BigDecimal.valueOf(totalParticipants), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(2, RoundingMode.HALF_UP) :
                    BigDecimal.ZERO;

            result.add(EducationBreakdownDTO.builder()
                    .educationLevel(educationLevel)
                    .count(count)
                    .percentage(percentage)
                    .build());
        }

        // Sort by count descending
        result.sort((a, b) -> Long.compare(b.getCount(), a.getCount()));

        return result;
    }

    /**
     * Calculates center-level breakdown.
     */
    private List<CenterAnalyticsDTO> calculateCenterBreakdown(
            List<Center> centers,
            List<Participant> participants,
            List<Enrollment> enrollments,
            List<Cohort> cohorts
    ) {
        List<CenterAnalyticsDTO> result = new ArrayList<>();

        for (Center center : centers) {
            UUID centerId = center.getId();

            // Count participants enrolled in cohorts at this center
            Set<UUID> participantIdsInCenter = cohorts.stream()
                    .filter(c -> c.getCenter() != null && c.getCenter().getId().equals(centerId))
                    .flatMap(c -> enrollments.stream()
                            .filter(e -> e.getCohort() != null && e.getCohort().getId().equals(c.getId()))
                            .map(e -> e.getParticipant().getId()))
                    .collect(Collectors.toSet());

            long totalParticipants = participantIdsInCenter.size();

            // Count enrollments in cohorts at this center
            long totalEnrollments = cohorts.stream()
                    .filter(c -> c.getCenter() != null && c.getCenter().getId().equals(centerId))
                    .mapToLong(c -> enrollments.stream()
                            .filter(e -> e.getCohort() != null && e.getCohort().getId().equals(c.getId()))
                            .count())
                    .sum();

            // Count active cohorts at this center
            long totalActiveCohorts = cohorts.stream()
                    .filter(c -> c.getCenter() != null && 
                               c.getCenter().getId().equals(centerId) &&
                               c.getStatus() == com.dseme.app.enums.CohortStatus.ACTIVE)
                    .count();

            result.add(CenterAnalyticsDTO.builder()
                    .centerId(centerId)
                    .centerName(center.getCenterName())
                    .partnerId(center.getPartner() != null ? center.getPartner().getPartnerId() : null)
                    .partnerName(center.getPartner() != null ? center.getPartner().getPartnerName() : "Unknown")
                    .region(center.getRegion())
                    .country(center.getCountry())
                    .location(center.getLocation())
                    .totalParticipants(totalParticipants)
                    .totalEnrollments(totalEnrollments)
                    .totalActiveCohorts(totalActiveCohorts)
                    .build());
        }

        // Sort by total participants descending
        result.sort((a, b) -> Long.compare(b.getTotalParticipants(), a.getTotalParticipants()));

        return result;
    }

    /**
     * Calculates region-level breakdown (aggregated across centers).
     */
    private List<RegionAnalyticsDTO> calculateRegionBreakdown(
            List<Center> centers,
            List<Participant> participants,
            List<Enrollment> enrollments,
            List<Cohort> cohorts
    ) {
        // Group centers by region
        Map<String, List<Center>> centersByRegion = centers.stream()
                .filter(c -> c.getRegion() != null && !c.getRegion().trim().isEmpty())
                .collect(Collectors.groupingBy(
                        c -> c.getRegion().trim() + "|" + (c.getCountry() != null ? c.getCountry() : "Unknown"),
                        Collectors.toList()
                ));

        List<RegionAnalyticsDTO> result = new ArrayList<>();

        for (Map.Entry<String, List<Center>> entry : centersByRegion.entrySet()) {
            String[] parts = entry.getKey().split("\\|");
            String region = parts[0];
            String country = parts.length > 1 ? parts[1] : "Unknown";
            List<Center> regionCenters = entry.getValue();

            Set<UUID> centerIds = regionCenters.stream()
                    .map(Center::getId)
                    .collect(Collectors.toSet());

            // Count participants in this region
            Set<UUID> participantIdsInRegion = cohorts.stream()
                    .filter(c -> c.getCenter() != null && centerIds.contains(c.getCenter().getId()))
                    .flatMap(c -> enrollments.stream()
                            .filter(e -> e.getCohort() != null && e.getCohort().getId().equals(c.getId()))
                            .map(e -> e.getParticipant().getId()))
                    .collect(Collectors.toSet());

            long totalParticipants = participantIdsInRegion.size();

            // Count enrollments in this region
            long totalEnrollments = cohorts.stream()
                    .filter(c -> c.getCenter() != null && centerIds.contains(c.getCenter().getId()))
                    .mapToLong(c -> enrollments.stream()
                            .filter(e -> e.getCohort() != null && e.getCohort().getId().equals(c.getId()))
                            .count())
                    .sum();

            // Count active cohorts in this region
            long totalActiveCohorts = cohorts.stream()
                    .filter(c -> c.getCenter() != null && 
                               centerIds.contains(c.getCenter().getId()) &&
                               c.getStatus() == com.dseme.app.enums.CohortStatus.ACTIVE)
                    .count();

            // Count unique partners in this region
            Set<String> partnerIds = regionCenters.stream()
                    .filter(c -> c.getPartner() != null)
                    .map(c -> c.getPartner().getPartnerId())
                    .collect(Collectors.toSet());

            result.add(RegionAnalyticsDTO.builder()
                    .region(region)
                    .country(country)
                    .totalParticipants(totalParticipants)
                    .totalEnrollments(totalEnrollments)
                    .totalActiveCohorts(totalActiveCohorts)
                    .totalCenters((long) regionCenters.size())
                    .totalPartners((long) partnerIds.size())
                    .build());
        }

        // Sort by total participants descending
        result.sort((a, b) -> Long.compare(b.getTotalParticipants(), a.getTotalParticipants()));

        return result;
    }

    /**
     * Calculates country-level breakdown (aggregated across regions).
     */
    private List<CountryAnalyticsDTO> calculateCountryBreakdown(
            List<Center> centers,
            List<Participant> participants,
            List<Enrollment> enrollments,
            List<Cohort> cohorts
    ) {
        // Group centers by country
        Map<String, List<Center>> centersByCountry = centers.stream()
                .filter(c -> c.getCountry() != null && !c.getCountry().trim().isEmpty())
                .collect(Collectors.groupingBy(
                        c -> c.getCountry().trim(),
                        Collectors.toList()
                ));

        List<CountryAnalyticsDTO> result = new ArrayList<>();

        for (Map.Entry<String, List<Center>> entry : centersByCountry.entrySet()) {
            String country = entry.getKey();
            List<Center> countryCenters = entry.getValue();

            Set<UUID> centerIds = countryCenters.stream()
                    .map(Center::getId)
                    .collect(Collectors.toSet());

            // Count participants in this country
            Set<UUID> participantIdsInCountry = cohorts.stream()
                    .filter(c -> c.getCenter() != null && centerIds.contains(c.getCenter().getId()))
                    .flatMap(c -> enrollments.stream()
                            .filter(e -> e.getCohort() != null && e.getCohort().getId().equals(c.getId()))
                            .map(e -> e.getParticipant().getId()))
                    .collect(Collectors.toSet());

            long totalParticipants = participantIdsInCountry.size();

            // Count enrollments in this country
            long totalEnrollments = cohorts.stream()
                    .filter(c -> c.getCenter() != null && centerIds.contains(c.getCenter().getId()))
                    .mapToLong(c -> enrollments.stream()
                            .filter(e -> e.getCohort() != null && e.getCohort().getId().equals(c.getId()))
                            .count())
                    .sum();

            // Count active cohorts in this country
            long totalActiveCohorts = cohorts.stream()
                    .filter(c -> c.getCenter() != null && 
                               centerIds.contains(c.getCenter().getId()) &&
                               c.getStatus() == com.dseme.app.enums.CohortStatus.ACTIVE)
                    .count();

            // Count unique regions in this country
            Set<String> regions = countryCenters.stream()
                    .filter(c -> c.getRegion() != null && !c.getRegion().trim().isEmpty())
                    .map(c -> c.getRegion().trim())
                    .collect(Collectors.toSet());

            // Count unique partners in this country
            Set<String> partnerIds = countryCenters.stream()
                    .filter(c -> c.getPartner() != null)
                    .map(c -> c.getPartner().getPartnerId())
                    .collect(Collectors.toSet());

            result.add(CountryAnalyticsDTO.builder()
                    .country(country)
                    .totalParticipants(totalParticipants)
                    .totalEnrollments(totalEnrollments)
                    .totalActiveCohorts(totalActiveCohorts)
                    .totalCenters((long) countryCenters.size())
                    .totalRegions((long) regions.size())
                    .totalPartners((long) partnerIds.size())
                    .build());
        }

        // Sort by total participants descending
        result.sort((a, b) -> Long.compare(b.getTotalParticipants(), a.getTotalParticipants()));

        return result;
    }

    /**
     * Gets survey impact summaries across all partners.
     * 
     * Includes:
     * - Completion rates per survey
     * - Aggregated sentiment (for rating scale questions)
     * - Positive response rates
     * 
     * No raw responses are exposed - only aggregated metrics.
     * 
     * @param context DONOR context
     * @return Survey impact summary DTO
     */
    public SurveyImpactSummaryDTO getSurveyImpactSummaries(DonorContext context) {
        // Get all surveys (portfolio-wide)
        List<Survey> allSurveys = surveyRepository.findAll();

        if (allSurveys.isEmpty()) {
            return SurveyImpactSummaryDTO.builder()
                    .totalSurveys(0L)
                    .surveySummaries(new ArrayList<>())
                    .overallCompletionRate(BigDecimal.ZERO)
                    .overallAverageSentiment(BigDecimal.ZERO)
                    .build();
        }

        // Get all survey responses
        List<SurveyResponse> allResponses = surveyResponseRepository.findAll();

        // Get all survey answers
        List<SurveyAnswer> allAnswers = surveyAnswerRepository.findAll();

        // Calculate summaries for each survey
        List<SurveySummaryDTO> surveySummaries = new ArrayList<>();
        long totalTargeted = 0;
        long totalSubmitted = 0;
        List<BigDecimal> sentimentScores = new ArrayList<>();

        for (Survey survey : allSurveys) {
            SurveySummaryDTO summary = calculateSurveySummary(survey, allResponses, allAnswers);
            surveySummaries.add(summary);

            totalTargeted += summary.getTotalTargeted();
            totalSubmitted += summary.getTotalSubmitted();

            if (summary.getAverageSentiment() != null && summary.getAverageSentiment().compareTo(BigDecimal.ZERO) > 0) {
                sentimentScores.add(summary.getAverageSentiment());
            }
        }

        // Calculate overall completion rate
        BigDecimal overallCompletionRate = totalTargeted > 0 ?
                BigDecimal.valueOf(totalSubmitted)
                        .divide(BigDecimal.valueOf(totalTargeted), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        // Calculate overall average sentiment
        BigDecimal overallAverageSentiment = BigDecimal.ZERO;
        if (!sentimentScores.isEmpty()) {
            BigDecimal sum = sentimentScores.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            overallAverageSentiment = sum.divide(
                    BigDecimal.valueOf(sentimentScores.size()),
                    2,
                    RoundingMode.HALF_UP
            );
        }

        // Sort surveys by completion rate descending
        surveySummaries.sort((a, b) -> b.getCompletionRate().compareTo(a.getCompletionRate()));

        return SurveyImpactSummaryDTO.builder()
                .totalSurveys((long) allSurveys.size())
                .surveySummaries(surveySummaries)
                .overallCompletionRate(overallCompletionRate)
                .overallAverageSentiment(overallAverageSentiment)
                .build();
    }

    /**
     * Calculates summary for a single survey.
     */
    private SurveySummaryDTO calculateSurveySummary(
            Survey survey,
            List<SurveyResponse> allResponses,
            List<SurveyAnswer> allAnswers
    ) {
        // Filter responses for this survey
        List<SurveyResponse> surveyResponses = allResponses.stream()
                .filter(r -> r.getSurvey() != null && r.getSurvey().getId().equals(survey.getId()))
                .collect(Collectors.toList());

        long totalTargeted = surveyResponses.size();
        long totalSubmitted = surveyResponses.stream()
                .filter(r -> r.getSubmittedAt() != null)
                .count();

        // Calculate completion rate
        BigDecimal completionRate = totalTargeted > 0 ?
                BigDecimal.valueOf(totalSubmitted)
                        .divide(BigDecimal.valueOf(totalTargeted), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        // Get submitted responses only
        List<SurveyResponse> submittedResponses = surveyResponses.stream()
                .filter(r -> r.getSubmittedAt() != null)
                .collect(Collectors.toList());

        // Get all questions for this survey
        List<com.dseme.app.models.SurveyQuestion> questions = survey.getQuestions();
        long totalQuestions = questions != null ? questions.size() : 0L;

        // Calculate average sentiment (for SCALE questions)
        BigDecimal averageSentiment = calculateAverageSentiment(
                submittedResponses, questions, allAnswers);

        // Calculate positive response rate
        BigDecimal positiveResponseRate = calculatePositiveResponseRate(
                submittedResponses, questions, allAnswers);

        return SurveySummaryDTO.builder()
                .surveyId(survey.getId())
                .surveyTitle(survey.getTitle())
                .surveyType(survey.getSurveyType() != null ? survey.getSurveyType().name() : "UNKNOWN")
                .partnerId(survey.getPartner() != null ? survey.getPartner().getPartnerId() : null)
                .partnerName(survey.getPartner() != null ? survey.getPartner().getPartnerName() : "Unknown")
                .cohortId(survey.getCohort() != null ? survey.getCohort().getId() : null)
                .cohortName(survey.getCohort() != null ? survey.getCohort().getCohortName() : null)
                .startDate(survey.getStartDate())
                .endDate(survey.getEndDate())
                .status(survey.getStatus() != null ? survey.getStatus().name() : "UNKNOWN")
                .totalTargeted(totalTargeted)
                .totalSubmitted(totalSubmitted)
                .completionRate(completionRate)
                .averageSentiment(averageSentiment)
                .positiveResponseRate(positiveResponseRate)
                .totalQuestions(totalQuestions)
                .build();
    }

    /**
     * Calculates average sentiment score from SCALE questions.
     */
    private BigDecimal calculateAverageSentiment(
            List<SurveyResponse> submittedResponses,
            List<com.dseme.app.models.SurveyQuestion> questions,
            List<SurveyAnswer> allAnswers
    ) {
        if (submittedResponses.isEmpty() || questions == null || questions.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Filter SCALE questions
        List<UUID> scaleQuestionIds = questions.stream()
                .filter(q -> q.getQuestionType() == QuestionType.SCALE)
                .map(com.dseme.app.models.SurveyQuestion::getId)
                .collect(Collectors.toList());

        if (scaleQuestionIds.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Get all answers for submitted responses to SCALE questions
        List<BigDecimal> scaleValues = new ArrayList<>();
        for (SurveyResponse response : submittedResponses) {
            List<SurveyAnswer> responseAnswers = allAnswers.stream()
                    .filter(a -> a.getResponse() != null &&
                               a.getResponse().getId().equals(response.getId()) &&
                               a.getQuestion() != null &&
                               scaleQuestionIds.contains(a.getQuestion().getId()))
                    .collect(Collectors.toList());

            for (SurveyAnswer answer : responseAnswers) {
                if (answer.getAnswerValue() != null && !answer.getAnswerValue().trim().isEmpty()) {
                    try {
                        BigDecimal value = new BigDecimal(answer.getAnswerValue().trim());
                        if (value.compareTo(BigDecimal.ZERO) >= 0) {
                            scaleValues.add(value);
                        }
                    } catch (NumberFormatException e) {
                        // Skip invalid numeric values
                    }
                }
            }
        }

        if (scaleValues.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal sum = scaleValues.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(
                BigDecimal.valueOf(scaleValues.size()),
                2,
                RoundingMode.HALF_UP
        );
    }

    /**
     * Calculates positive response rate.
     * For SCALE questions: considers values >= midpoint as positive.
     * For SINGLE_CHOICE/MULTIPLE_CHOICE: considers "YES", "yes", "Yes", "POSITIVE", etc. as positive.
     */
    private BigDecimal calculatePositiveResponseRate(
            List<SurveyResponse> submittedResponses,
            List<com.dseme.app.models.SurveyQuestion> questions,
            List<SurveyAnswer> allAnswers
    ) {
        if (submittedResponses.isEmpty() || questions == null || questions.isEmpty()) {
            return BigDecimal.ZERO;
        }

        long totalAnswers = 0;
        long positiveAnswers = 0;

        for (SurveyResponse response : submittedResponses) {
            List<SurveyAnswer> responseAnswers = allAnswers.stream()
                    .filter(a -> a.getResponse() != null &&
                               a.getResponse().getId().equals(response.getId()))
                    .collect(Collectors.toList());

            for (SurveyAnswer answer : responseAnswers) {
                if (answer.getQuestion() == null || answer.getAnswerValue() == null) {
                    continue;
                }

                totalAnswers++;
                String answerValue = answer.getAnswerValue().trim().toUpperCase();

                QuestionType questionType = answer.getQuestion().getQuestionType();

                if (questionType == QuestionType.SCALE) {
                    try {
                        BigDecimal value = new BigDecimal(answer.getAnswerValue().trim());
                        // Assume scale is 0-5 or 0-10, midpoint is positive
                        // For 0-5: >= 3 is positive, for 0-10: >= 5 is positive
                        // We'll use >= 3 as threshold (works for both scales)
                        if (value.compareTo(BigDecimal.valueOf(3)) >= 0) {
                            positiveAnswers++;
                        }
                    } catch (NumberFormatException e) {
                        // Skip invalid numeric values
                    }
                } else if (questionType == QuestionType.SINGLE_CHOICE || questionType == QuestionType.MULTIPLE_CHOICE) {
                    // Check for positive keywords
                    if (answerValue.contains("YES") ||
                        answerValue.contains("POSITIVE") ||
                        answerValue.contains("AGREE") ||
                        answerValue.contains("SATISFIED") ||
                        answerValue.contains("GOOD") ||
                        answerValue.contains("EXCELLENT") ||
                        answerValue.contains("VERY GOOD")) {
                        positiveAnswers++;
                    }
                }
            }
        }

        if (totalAnswers == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(positiveAnswers)
                .divide(BigDecimal.valueOf(totalAnswers), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
