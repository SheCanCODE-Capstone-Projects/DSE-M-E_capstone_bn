package com.dseme.app.services.facilitator;

import com.dseme.app.dtos.facilitator.*;
import com.dseme.app.enums.AttendanceStatus;
import com.dseme.app.enums.EnrollmentStatus;
import com.dseme.app.models.*;
import com.dseme.app.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for generating reports and analytics.
 * Provides attendance trends, grade trends, participant progress, and cohort performance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportsService {

    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final ScoreRepository scoreRepository;
    private final TrainingModuleRepository trainingModuleRepository;
    private final CohortIsolationService cohortIsolationService;

    /**
     * Gets attendance trends for a date range.
     */
    public AttendanceTrendDTO getAttendanceTrends(
            FacilitatorContext context,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Cohort activeCohort = cohortIsolationService.getFacilitatorActiveCohort(context);
        List<Enrollment> enrollments = enrollmentRepository.findByCohortId(context.getCohortId());

        // Get all attendance records in the date range
        List<Attendance> attendances = attendanceRepository.findByCohortIdAndSessionDateBetween(
                context.getCohortId(), startDate, endDate);

        // Group by date
        List<AttendanceTrendDTO.DailyAttendance> dailyAttendance = new ArrayList<>();
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            LocalDate finalCurrentDate = currentDate;
            List<Attendance> dayAttendances = attendances.stream()
                    .filter(a -> a.getSessionDate().equals(finalCurrentDate))
                    .collect(Collectors.toList());

            long presentCount = dayAttendances.stream()
                    .filter(a -> a.getStatus() == AttendanceStatus.PRESENT)
                    .count();
            long absentCount = dayAttendances.stream()
                    .filter(a -> a.getStatus() == AttendanceStatus.ABSENT)
                    .count();
            long lateCount = dayAttendances.stream()
                    .filter(a -> a.getStatus() == AttendanceStatus.LATE)
                    .count();
            long excusedCount = dayAttendances.stream()
                    .filter(a -> a.getStatus() == AttendanceStatus.EXCUSED)
                    .count();

            long totalParticipants = enrollments.size();
            long attendedCount = presentCount + lateCount + excusedCount;
            BigDecimal attendanceRate = totalParticipants > 0 ?
                    BigDecimal.valueOf(attendedCount)
                            .divide(BigDecimal.valueOf(totalParticipants), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(2, RoundingMode.HALF_UP) :
                    BigDecimal.ZERO;

            dailyAttendance.add(AttendanceTrendDTO.DailyAttendance.builder()
                    .date(currentDate)
                    .presentCount(presentCount)
                    .absentCount(absentCount)
                    .lateCount(lateCount)
                    .excusedCount(excusedCount)
                    .attendanceRate(attendanceRate)
                    .totalParticipants(totalParticipants)
                    .build());

            currentDate = currentDate.plusDays(1);
        }

        // Calculate totals
        long totalPresent = attendances.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.PRESENT)
                .count();
        long totalAbsent = attendances.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.ABSENT)
                .count();
        long totalLate = attendances.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.LATE)
                .count();
        long totalExcused = attendances.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.EXCUSED)
                .count();

        BigDecimal averageAttendanceRate = dailyAttendance.stream()
                .map(AttendanceTrendDTO.DailyAttendance::getAttendanceRate)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(dailyAttendance.size()), 2, RoundingMode.HALF_UP);

        return AttendanceTrendDTO.builder()
                .cohortId(context.getCohortId())
                .cohortName(activeCohort.getCohortName())
                .startDate(startDate)
                .endDate(endDate)
                .dailyAttendance(dailyAttendance)
                .averageAttendanceRate(averageAttendanceRate)
                .totalSessions((long) attendances.size())
                .totalPresent(totalPresent)
                .totalAbsent(totalAbsent)
                .totalLate(totalLate)
                .totalExcused(totalExcused)
                .build();
    }

    /**
     * Gets grade trends for a module.
     */
    public GradeTrendDTO getGradeTrends(FacilitatorContext context, UUID moduleId) {
        cohortIsolationService.getFacilitatorActiveCohort(context);
        List<Enrollment> enrollments = enrollmentRepository.findByCohortId(context.getCohortId());

        TrainingModule module = trainingModuleRepository.findById(moduleId)
                .orElseThrow(() -> new RuntimeException("Module not found"));

        // Get all scores for this module
        List<Score> scores = enrollments.stream()
                .flatMap(e -> scoreRepository.findByEnrollmentIdAndModuleId(e.getId(), moduleId).stream())
                .collect(Collectors.toList());

        // Group by assessment date
        List<GradeTrendDTO.AssessmentTrend> assessmentTrends = scores.stream()
                .collect(Collectors.groupingBy(
                        score -> score.getAssessmentDate() != null ? score.getAssessmentDate() :
                                (score.getRecordedAt() != null ?
                                        score.getRecordedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate() :
                                        LocalDate.now())
                ))
                .entrySet().stream()
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    List<Score> dateScores = entry.getValue();

                    BigDecimal averageScore = dateScores.stream()
                            .map(Score::getScoreValue)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(dateScores.size()), 2, RoundingMode.HALF_UP);

                    BigDecimal maxScore = dateScores.stream()
                            .map(Score::getMaxScore)
                            .findFirst()
                            .orElse(new BigDecimal("100.0"));

                    String assessmentName = dateScores.stream()
                            .map(Score::getAssessmentName)
                            .filter(name -> name != null && !name.isEmpty())
                            .findFirst()
                            .orElse("Assessment");

                    return GradeTrendDTO.AssessmentTrend.builder()
                            .assessmentDate(date)
                            .assessmentName(assessmentName)
                            .averageScore(averageScore)
                            .participantCount((long) dateScores.size())
                            .maxScore(maxScore)
                            .build();
                })
                .sorted((a, b) -> a.getAssessmentDate().compareTo(b.getAssessmentDate()))
                .collect(Collectors.toList());

        BigDecimal overallAverage = scores.isEmpty() ? BigDecimal.ZERO :
                scores.stream()
                        .map(Score::getScoreValue)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);

        return GradeTrendDTO.builder()
                .moduleId(moduleId)
                .moduleName(module.getModuleName())
                .cohortId(context.getCohortId())
                .cohortName(cohortIsolationService.getFacilitatorActiveCohort(context).getCohortName())
                .assessmentTrends(assessmentTrends)
                .overallAverage(overallAverage)
                .totalAssessments((long) scores.size())
                .build();
    }

    /**
     * Gets participant progress report.
     */
    public ParticipantProgressDTO getParticipantProgress(FacilitatorContext context, UUID participantId) {
        cohortIsolationService.getFacilitatorActiveCohort(context);
        Cohort activeCohort = cohortIsolationService.getFacilitatorActiveCohort(context);

        Enrollment enrollment = enrollmentRepository.findByParticipantIdAndCohortId(participantId, context.getCohortId())
                .orElseThrow(() -> new RuntimeException("Participant not enrolled in this cohort"));

        Participant participant = enrollment.getParticipant();

        // Get all modules
        List<TrainingModule> modules = trainingModuleRepository.findByProgramId(activeCohort.getProgram().getId());

        // Calculate module progress
        List<ParticipantProgressDTO.ModuleProgress> moduleProgress = modules.stream()
                .map(module -> {
                    List<Attendance> moduleAttendances = attendanceRepository.findByEnrollmentId(enrollment.getId())
                            .stream()
                            .filter(a -> a.getModule().getId().equals(module.getId()))
                            .collect(Collectors.toList());

                    BigDecimal attendancePercentage = calculateAttendancePercentage(moduleAttendances);

                    List<Score> moduleScores = scoreRepository.findByEnrollmentIdAndModuleId(enrollment.getId(), module.getId());
                    BigDecimal gradeAverage = moduleScores.isEmpty() ? BigDecimal.ZERO :
                            moduleScores.stream()
                                    .map(Score::getScoreValue)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                                    .divide(BigDecimal.valueOf(moduleScores.size()), 2, RoundingMode.HALF_UP);

                    return ParticipantProgressDTO.ModuleProgress.builder()
                            .moduleId(module.getId())
                            .moduleName(module.getModuleName())
                            .attendancePercentage(attendancePercentage)
                            .gradeAverage(gradeAverage)
                            .isCompleted(!moduleAttendances.isEmpty() && !moduleScores.isEmpty())
                            .build();
                })
                .collect(Collectors.toList());

        // Calculate overall stats
        List<Attendance> allAttendances = attendanceRepository.findByEnrollmentId(enrollment.getId());
        BigDecimal overallAttendancePercentage = calculateAttendancePercentage(allAttendances);

        List<Score> allScores = modules.stream()
                .flatMap(m -> scoreRepository.findByEnrollmentIdAndModuleId(enrollment.getId(), m.getId()).stream())
                .collect(Collectors.toList());
        BigDecimal overallGradeAverage = allScores.isEmpty() ? BigDecimal.ZERO :
                allScores.stream()
                        .map(Score::getScoreValue)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(allScores.size()), 2, RoundingMode.HALF_UP);

        long completedModules = moduleProgress.stream()
                .filter(ParticipantProgressDTO.ModuleProgress::getIsCompleted)
                .count();

        return ParticipantProgressDTO.builder()
                .participantId(participant.getId())
                .participantName(participant.getFirstName() + " " + participant.getLastName())
                .email(participant.getEmail())
                .enrollmentId(enrollment.getId())
                .cohortId(context.getCohortId())
                .cohortName(activeCohort.getCohortName())
                .overallAttendancePercentage(overallAttendancePercentage)
                .overallGradeAverage(overallGradeAverage)
                .completedModules(completedModules)
                .totalModules((long) modules.size())
                .moduleProgress(moduleProgress)
                .build();
    }

    /**
     * Gets cohort performance summary.
     */
    public CohortPerformanceDTO getCohortPerformance(FacilitatorContext context) {
        Cohort activeCohort = cohortIsolationService.getFacilitatorActiveCohort(context);
        List<Enrollment> enrollments = enrollmentRepository.findByCohortId(context.getCohortId());
        List<Enrollment> activeEnrollments = enrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ENROLLED || e.getStatus() == EnrollmentStatus.ACTIVE)
                .collect(Collectors.toList());

        List<TrainingModule> modules = trainingModuleRepository.findByProgramId(activeCohort.getProgram().getId());

        // Calculate module performance
        List<CohortPerformanceDTO.ModulePerformance> modulePerformance = modules.stream()
                .map(module -> {
                    List<Attendance> moduleAttendances = enrollments.stream()
                            .flatMap(e -> attendanceRepository.findByEnrollmentId(e.getId()).stream())
                            .filter(a -> a.getModule().getId().equals(module.getId()))
                            .collect(Collectors.toList());

                    BigDecimal attendanceRate = calculateAttendancePercentage(moduleAttendances);

                    List<Score> moduleScores = enrollments.stream()
                            .flatMap(e -> scoreRepository.findByEnrollmentIdAndModuleId(e.getId(), module.getId()).stream())
                            .collect(Collectors.toList());

                    BigDecimal averageGrade = moduleScores.isEmpty() ? BigDecimal.ZERO :
                            moduleScores.stream()
                                    .map(Score::getScoreValue)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                                    .divide(BigDecimal.valueOf(moduleScores.size()), 2, RoundingMode.HALF_UP);

                    long participantsCompleted = enrollments.stream()
                            .filter(e -> !scoreRepository.findByEnrollmentIdAndModuleId(e.getId(), module.getId()).isEmpty())
                            .count();

                    return CohortPerformanceDTO.ModulePerformance.builder()
                            .moduleId(module.getId())
                            .moduleName(module.getModuleName())
                            .averageAttendanceRate(attendanceRate)
                            .averageGrade(averageGrade)
                            .participantsCompleted(participantsCompleted)
                            .build();
                })
                .collect(Collectors.toList());

        // Calculate overall averages
        List<Attendance> allAttendances = enrollments.stream()
                .flatMap(e -> attendanceRepository.findByEnrollmentId(e.getId()).stream())
                .collect(Collectors.toList());
        BigDecimal averageAttendanceRate = calculateAttendancePercentage(allAttendances);

        List<Score> allScores = enrollments.stream()
                .flatMap(e -> modules.stream()
                        .flatMap(m -> scoreRepository.findByEnrollmentIdAndModuleId(e.getId(), m.getId()).stream()))
                .collect(Collectors.toList());
        BigDecimal averageGrade = allScores.isEmpty() ? BigDecimal.ZERO :
                allScores.stream()
                        .map(Score::getScoreValue)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(allScores.size()), 2, RoundingMode.HALF_UP);

        // Get top performers and needs attention
        List<CohortPerformanceDTO.ParticipantPerformance> participantPerformances = enrollments.stream()
                .map(enrollment -> {
                    List<Attendance> participantAttendances = attendanceRepository.findByEnrollmentId(enrollment.getId());
                    BigDecimal attendancePercentage = calculateAttendancePercentage(participantAttendances);

                    List<Score> participantScores = modules.stream()
                            .flatMap(m -> scoreRepository.findByEnrollmentIdAndModuleId(enrollment.getId(), m.getId()).stream())
                            .collect(Collectors.toList());
                    BigDecimal overallGrade = participantScores.isEmpty() ? BigDecimal.ZERO :
                            participantScores.stream()
                                    .map(Score::getScoreValue)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                                    .divide(BigDecimal.valueOf(participantScores.size()), 2, RoundingMode.HALF_UP);

                    return CohortPerformanceDTO.ParticipantPerformance.builder()
                            .participantId(enrollment.getParticipant().getId())
                            .participantName(enrollment.getParticipant().getFirstName() + " " + enrollment.getParticipant().getLastName())
                            .overallGrade(overallGrade)
                            .attendancePercentage(attendancePercentage)
                            .build();
                })
                .collect(Collectors.toList());

        List<CohortPerformanceDTO.ParticipantPerformance> topPerformers = participantPerformances.stream()
                .filter(p -> p.getOverallGrade().compareTo(new BigDecimal("80")) >= 0)
                .sorted((a, b) -> b.getOverallGrade().compareTo(a.getOverallGrade()))
                .limit(10)
                .collect(Collectors.toList());

        List<CohortPerformanceDTO.ParticipantPerformance> needsAttention = participantPerformances.stream()
                .filter(p -> p.getOverallGrade().compareTo(new BigDecimal("60")) <= 0)
                .sorted((a, b) -> a.getOverallGrade().compareTo(b.getOverallGrade()))
                .limit(10)
                .collect(Collectors.toList());

        long completedModules = modulePerformance.stream()
                .filter(m -> m.getParticipantsCompleted() > 0)
                .count();

        return CohortPerformanceDTO.builder()
                .cohortId(context.getCohortId())
                .cohortName(activeCohort.getCohortName())
                .totalParticipants((long) enrollments.size())
                .activeParticipants((long) activeEnrollments.size())
                .averageAttendanceRate(averageAttendanceRate)
                .averageGrade(averageGrade)
                .completedModules(completedModules)
                .totalModules((long) modules.size())
                .modulePerformance(modulePerformance)
                .topPerformers(topPerformers)
                .needsAttention(needsAttention)
                .build();
    }

    /**
     * Calculates attendance percentage from a list of attendances.
     */
    private BigDecimal calculateAttendancePercentage(List<Attendance> attendances) {
        if (attendances.isEmpty()) {
            return BigDecimal.ZERO;
        }

        long presentCount = attendances.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.PRESENT ||
                           a.getStatus() == AttendanceStatus.LATE ||
                           a.getStatus() == AttendanceStatus.EXCUSED)
                .count();

        return BigDecimal.valueOf(presentCount)
                .divide(BigDecimal.valueOf(attendances.size()), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
}

