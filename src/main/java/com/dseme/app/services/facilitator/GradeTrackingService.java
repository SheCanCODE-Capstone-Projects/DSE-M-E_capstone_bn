package com.dseme.app.services.facilitator;

import com.dseme.app.dtos.facilitator.*;
import com.dseme.app.enums.EnrollmentStatus;
import com.dseme.app.exceptions.AccessDeniedException;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.models.Enrollment;
import com.dseme.app.models.Score;
import com.dseme.app.models.TrainingModule;
import com.dseme.app.repositories.AttendanceRepository;
import com.dseme.app.repositories.EnrollmentRepository;
import com.dseme.app.repositories.ScoreRepository;
import com.dseme.app.repositories.TrainingModuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for grade tracking and management.
 * Handles grade statistics, high performers, need attention, and participant grade details.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GradeTrackingService {

    private final ScoreRepository scoreRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TrainingModuleRepository trainingModuleRepository;
    private final AttendanceRepository attendanceRepository;
    private final CohortIsolationService cohortIsolationService;

    /**
     * Gets grade statistics for a training module.
     * 
     * @param context Facilitator context
     * @param moduleId Training module ID
     * @return Grade statistics (class average, high performers, need attention, total assessments)
     */
    public GradeStatsDTO getGradeStats(FacilitatorContext context, UUID moduleId) {
        // Validate facilitator has active cohort
        cohortIsolationService.getFacilitatorActiveCohort(context);

        // Load module
        TrainingModule module = trainingModuleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Training module not found with ID: " + moduleId
                ));

        // Validate module belongs to facilitator's active cohort's program
        if (!module.getProgram().getId().equals(context.getCohort().getProgram().getId())) {
            throw new AccessDeniedException(
                "Access denied. Module does not belong to your active cohort's program."
            );
        }

        // Get all enrollments for the active cohort
        List<Enrollment> enrollments = enrollmentRepository.findByCohortId(context.getCohortId());

        // Get all scores for this module
        List<Score> allScores = scoreRepository.findByModuleId(moduleId);

        // Calculate class average (average of all scores)
        BigDecimal classAverage = calculateClassAverage(allScores);

        // Get unique assessment count (distinct combinations of assessmentType + assessmentName)
        long totalAssessments = allScores.stream()
                .map(s -> s.getAssessmentType() + ":" + (s.getAssessmentName() != null ? s.getAssessmentName() : ""))
                .distinct()
                .count();

        // Calculate high performers and need attention
        Map<UUID, BigDecimal> participantAverages = calculateParticipantAverages(enrollments, allScores);
        
        long highPerformersCount = participantAverages.values().stream()
                .filter(avg -> avg.compareTo(new BigDecimal("80.0")) >= 0)
                .count();

        long needAttentionCount = participantAverages.values().stream()
                .filter(avg -> avg.compareTo(new BigDecimal("60.0")) <= 0)
                .count();

        return GradeStatsDTO.builder()
                .moduleId(moduleId)
                .moduleName(module.getModuleName())
                .cohortId(context.getCohortId())
                .cohortName(context.getCohort().getCohortName())
                .classAverage(classAverage)
                .highPerformersCount(highPerformersCount)
                .needAttentionCount(needAttentionCount)
                .totalAssessments(totalAssessments)
                .totalParticipants((long) enrollments.size())
                .build();
    }

    /**
     * Gets list of high performers (overall average >= 80%).
     * 
     * @param context Facilitator context
     * @param moduleId Training module ID
     * @return List of high performer participants
     */
    public List<ParticipantGradeSummaryDTO> getHighPerformers(
            FacilitatorContext context,
            UUID moduleId
    ) {
        return getParticipantsByGradeThreshold(context, moduleId, new BigDecimal("80.0"), true);
    }

    /**
     * Gets list of participants needing attention (overall average <= 60%).
     * 
     * @param context Facilitator context
     * @param moduleId Training module ID
     * @return List of participants needing attention
     */
    public List<ParticipantGradeSummaryDTO> getNeedAttention(
            FacilitatorContext context,
            UUID moduleId
    ) {
        return getParticipantsByGradeThreshold(context, moduleId, new BigDecimal("60.0"), false);
    }

    /**
     * Searches participants by name and returns their grade summary.
     * 
     * @param context Facilitator context
     * @param moduleId Training module ID
     * @param searchTerm Search term (name)
     * @return List of matching participants with grade summaries
     */
    public List<ParticipantGradeSummaryDTO> searchParticipantsByName(
            FacilitatorContext context,
            UUID moduleId,
            String searchTerm
    ) {
        // Validate facilitator has active cohort
        cohortIsolationService.getFacilitatorActiveCohort(context);

        // Load module
        TrainingModule module = trainingModuleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Training module not found with ID: " + moduleId
                ));

        // Validate module belongs to facilitator's active cohort's program
        if (!module.getProgram().getId().equals(context.getCohort().getProgram().getId())) {
            throw new AccessDeniedException(
                "Access denied. Module does not belong to your active cohort's program."
            );
        }

        // Get all enrollments for the active cohort
        List<Enrollment> enrollments = enrollmentRepository.findByCohortId(context.getCohortId());

        // Filter by search term (name)
        String searchLower = searchTerm.toLowerCase().trim();
        List<Enrollment> filteredEnrollments = enrollments.stream()
                .filter(e -> {
                    String firstName = e.getParticipant().getFirstName() != null ? 
                            e.getParticipant().getFirstName().toLowerCase() : "";
                    String lastName = e.getParticipant().getLastName() != null ? 
                            e.getParticipant().getLastName().toLowerCase() : "";
                    return firstName.contains(searchLower) || lastName.contains(searchLower);
                })
                .collect(Collectors.toList());

        // Get all scores for this module
        List<Score> allScores = scoreRepository.findByModuleId(moduleId);

        // Get unique assessments in this module
        Set<String> uniqueAssessments = allScores.stream()
                .map(s -> s.getAssessmentType() + ":" + (s.getAssessmentName() != null ? s.getAssessmentName() : ""))
                .collect(Collectors.toSet());

        // Build participant grade summaries
        return filteredEnrollments.stream()
                .map(enrollment -> buildParticipantGradeSummary(
                        enrollment, allScores, uniqueAssessments))
                .collect(Collectors.toList());
    }

    /**
     * Gets detailed grade information for a specific participant.
     * 
     * @param context Facilitator context
     * @param enrollmentId Enrollment ID
     * @param moduleId Training module ID
     * @return Detailed participant grade information
     */
    public ParticipantGradeDetailDTO getParticipantGradeDetail(
            FacilitatorContext context,
            UUID enrollmentId,
            UUID moduleId
    ) {
        // Validate facilitator has active cohort
        cohortIsolationService.getFacilitatorActiveCohort(context);

        // Load enrollment
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Enrollment not found with ID: " + enrollmentId
                ));

        // Validate enrollment belongs to facilitator's active cohort
        if (!enrollment.getCohort().getId().equals(context.getCohortId())) {
            throw new AccessDeniedException(
                "Access denied. Enrollment does not belong to your assigned active cohort."
            );
        }

        // Load module
        TrainingModule module = trainingModuleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Training module not found with ID: " + moduleId
                ));

        // Get all scores for this enrollment and module
        List<Score> participantScores = scoreRepository.findByEnrollmentIdAndModuleId(enrollmentId, moduleId);

        // Get all scores for this module (to find unique assessments)
        List<Score> allModuleScores = scoreRepository.findByModuleId(moduleId);
        Set<String> uniqueAssessments = allModuleScores.stream()
                .map(s -> s.getAssessmentType() + ":" + (s.getAssessmentName() != null ? s.getAssessmentName() : ""))
                .collect(Collectors.toSet());

        // Get participant's assessment keys
        Set<String> participantAssessmentKeys = participantScores.stream()
                .map(s -> s.getAssessmentType() + ":" + (s.getAssessmentName() != null ? s.getAssessmentName() : ""))
                .collect(Collectors.toSet());

        // Calculate missing assessments
        long missingAssessmentsCount = uniqueAssessments.size() - participantAssessmentKeys.size();

        // Calculate overall grade
        BigDecimal overallGrade = calculateOverallGrade(participantScores);

        // Build assessment score DTOs
        List<ParticipantGradeDetailDTO.AssessmentScoreDTO> assessmentDTOs = participantScores.stream()
                .map(score -> {
                    // Prioritize assessmentDate over recordedAt for display
                    java.time.LocalDate assessmentDate = score.getAssessmentDate();
                    if (assessmentDate == null && score.getRecordedAt() != null) {
                        // Fallback to recordedAt date if assessmentDate is null
                        assessmentDate = score.getRecordedAt()
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate();
                    }
                    
                    return ParticipantGradeDetailDTO.AssessmentScoreDTO.builder()
                            .scoreId(score.getId())
                            .assessmentType(score.getAssessmentType())
                            .assessmentName(score.getAssessmentName())
                            .score(score.getScoreValue())
                            .maxScore(score.getMaxScore())
                            .assessmentDate(assessmentDate) // Prioritized date
                            .recordedAt(score.getRecordedAt()) // Fallback timestamp
                            .moduleId(module.getId())
                            .moduleName(module.getModuleName())
                            .build();
                })
                .sorted(Comparator.comparing(
                        (ParticipantGradeDetailDTO.AssessmentScoreDTO dto) -> 
                                dto.getAssessmentDate() != null ? dto.getAssessmentDate() :
                                (dto.getRecordedAt() != null ? 
                                        dto.getRecordedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate() :
                                        java.time.LocalDate.MIN)
                ).reversed())
                .collect(Collectors.toList());

        // Get display status
        String displayStatus = getDisplayStatus(enrollment);

        return ParticipantGradeDetailDTO.builder()
                .participantId(enrollment.getParticipant().getId())
                .firstName(enrollment.getParticipant().getFirstName())
                .lastName(enrollment.getParticipant().getLastName())
                .email(enrollment.getParticipant().getEmail())
                .cohortName(enrollment.getCohort().getCohortName())
                .enrollmentStatus(displayStatus)
                .enrollmentId(enrollment.getId())
                .cohortId(enrollment.getCohort().getId())
                .assessments(assessmentDTOs)
                .overallGrade(overallGrade)
                .missingAssessmentsCount(missingAssessmentsCount)
                .totalAssessments((long) uniqueAssessments.size())
                .build();
    }

    /**
     * Gets participants by grade threshold (high performers or need attention).
     */
    private List<ParticipantGradeSummaryDTO> getParticipantsByGradeThreshold(
            FacilitatorContext context,
            UUID moduleId,
            BigDecimal threshold,
            boolean isHighPerformers // true for >= threshold, false for <= threshold
    ) {
        // Validate facilitator has active cohort
        cohortIsolationService.getFacilitatorActiveCohort(context);

        // Load module
        TrainingModule module = trainingModuleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Training module not found with ID: " + moduleId
                ));

        // Validate module belongs to facilitator's active cohort's program
        if (!module.getProgram().getId().equals(context.getCohort().getProgram().getId())) {
            throw new AccessDeniedException(
                "Access denied. Module does not belong to your active cohort's program."
            );
        }

        // Get all enrollments for the active cohort
        List<Enrollment> enrollments = enrollmentRepository.findByCohortId(context.getCohortId());

        // Get all scores for this module
        List<Score> allScores = scoreRepository.findByModuleId(moduleId);

        // Get unique assessments in this module
        Set<String> uniqueAssessments = allScores.stream()
                .map(s -> s.getAssessmentType() + ":" + (s.getAssessmentName() != null ? s.getAssessmentName() : ""))
                .collect(Collectors.toSet());

        // Build participant grade summaries and filter by threshold
        return enrollments.stream()
                .map(enrollment -> buildParticipantGradeSummary(enrollment, allScores, uniqueAssessments))
                .filter(summary -> {
                    if (summary.getOverallGrade() == null) {
                        return false; // Exclude participants with no scores
                    }
                    if (isHighPerformers) {
                        return summary.getOverallGrade().compareTo(threshold) >= 0;
                    } else {
                        return summary.getOverallGrade().compareTo(threshold) <= 0;
                    }
                })
                .sorted(Comparator.comparing(
                        ParticipantGradeSummaryDTO::getOverallGrade,
                        Comparator.nullsLast(Comparator.reverseOrder()))) // Sort by grade descending
                .collect(Collectors.toList());
    }

    /**
     * Builds participant grade summary from enrollment and scores.
     */
    private ParticipantGradeSummaryDTO buildParticipantGradeSummary(
            Enrollment enrollment,
            List<Score> allScores,
            Set<String> uniqueAssessments
    ) {
        // Get scores for this enrollment
        List<Score> participantScores = allScores.stream()
                .filter(s -> s.getEnrollment().getId().equals(enrollment.getId()))
                .collect(Collectors.toList());

        // Calculate overall grade
        BigDecimal overallGrade = calculateOverallGrade(participantScores);

        // Calculate missing assessments
        Set<String> participantAssessmentKeys = participantScores.stream()
                .map(s -> s.getAssessmentType() + ":" + (s.getAssessmentName() != null ? s.getAssessmentName() : ""))
                .collect(Collectors.toSet());

        long missingAssessmentsCount = uniqueAssessments.size() - participantAssessmentKeys.size();

        return ParticipantGradeSummaryDTO.builder()
                .enrollmentId(enrollment.getId())
                .participantId(enrollment.getParticipant().getId())
                .firstName(enrollment.getParticipant().getFirstName())
                .lastName(enrollment.getParticipant().getLastName())
                .email(enrollment.getParticipant().getEmail())
                .overallGrade(overallGrade)
                .missingAssessmentsCount(missingAssessmentsCount)
                .totalAssessments((long) uniqueAssessments.size())
                .build();
    }

    /**
     * Calculates class average (average of all scores).
     */
    private BigDecimal calculateClassAverage(List<Score> allScores) {
        if (allScores.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal sum = allScores.stream()
                .map(Score::getScoreValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return sum.divide(BigDecimal.valueOf(allScores.size()), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates overall grade for a participant (average of all their scores).
     */
    private BigDecimal calculateOverallGrade(List<Score> participantScores) {
        if (participantScores.isEmpty()) {
            return null; // No scores yet
        }

        BigDecimal sum = participantScores.stream()
                .map(Score::getScoreValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return sum.divide(BigDecimal.valueOf(participantScores.size()), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates participant averages map.
     */
    private Map<UUID, BigDecimal> calculateParticipantAverages(
            List<Enrollment> enrollments,
            List<Score> allScores
    ) {
        Map<UUID, BigDecimal> averages = new HashMap<>();

        for (Enrollment enrollment : enrollments) {
            List<Score> participantScores = allScores.stream()
                    .filter(s -> s.getEnrollment().getId().equals(enrollment.getId()))
                    .collect(Collectors.toList());

            BigDecimal average = calculateOverallGrade(participantScores);
            if (average != null) {
                averages.put(enrollment.getId(), average);
            }
        }

        return averages;
    }

    /**
     * Gets display status for enrollment.
     * Returns "INACTIVE" for ENROLLED status after 2-week gap, otherwise returns status name.
     */
    private String getDisplayStatus(Enrollment enrollment) {
        if (enrollment.getStatus() == EnrollmentStatus.ENROLLED) {
            // Check if there's a 2-week gap (meaning they were ACTIVE before)
            java.time.LocalDate mostRecentAttendance = attendanceRepository
                    .findMostRecentAttendanceDateByEnrollmentId(enrollment.getId());
            
            if (mostRecentAttendance != null) {
                long daysSinceLastAttendance = java.time.temporal.ChronoUnit.DAYS.between(
                        mostRecentAttendance, java.time.LocalDate.now());
                if (daysSinceLastAttendance >= 14) {
                    return "INACTIVE";
                }
            }
        }
        
        return enrollment.getStatus().name();
    }
}

