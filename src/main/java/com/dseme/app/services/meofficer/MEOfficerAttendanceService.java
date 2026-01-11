package com.dseme.app.services.meofficer;

import com.dseme.app.dtos.meofficer.AttendanceSummaryRequestDTO;
import com.dseme.app.dtos.meofficer.AttendanceSummaryResponseDTO;
import com.dseme.app.dtos.meofficer.MEOfficerContext;
import com.dseme.app.enums.AttendanceStatus;
import com.dseme.app.models.Attendance;
import com.dseme.app.repositories.AttendanceRepository;
import com.dseme.app.repositories.CohortRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for ME_OFFICER attendance operations.
 * 
 * Enforces strict partner-level data isolation.
 * All queries filter by partner_id from MEOfficerContext.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MEOfficerAttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final CohortRepository cohortRepository;
    private final MEOfficerAuthorizationService meOfficerAuthorizationService;

    /**
     * Gets attendance summary for ME_OFFICER's partner.
     * Includes aggregated metrics: attendance rate, absentee trends.
     * 
     * @param context ME_OFFICER context
     * @param request Summary request with optional cohort filter
     * @return Attendance summary response
     */
    public AttendanceSummaryResponseDTO getAttendanceSummary(
            MEOfficerContext context,
            AttendanceSummaryRequestDTO request
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        // Get attendance records filtered by partner (and cohort if specified)
        List<Attendance> attendances;
        if (request.getCohortId() != null) {
            attendances = attendanceRepository.findByParticipantPartnerPartnerIdAndCohortId(
                    context.getPartnerId(),
                    request.getCohortId()
            );
        } else {
            attendances = attendanceRepository.findByParticipantPartnerPartnerId(
                    context.getPartnerId()
            );
        }

        if (attendances.isEmpty()) {
            return buildEmptySummary(request.getCohortId());
        }

        // Calculate overall metrics
        long totalRecords = attendances.size();
        long presentRecords = attendances.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.PRESENT ||
                           a.getStatus() == AttendanceStatus.LATE ||
                           a.getStatus() == AttendanceStatus.EXCUSED)
                .count();
        long absentRecords = attendances.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.ABSENT)
                .count();

        BigDecimal attendanceRate = totalRecords > 0 ?
                BigDecimal.valueOf(presentRecords)
                        .divide(BigDecimal.valueOf(totalRecords), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        // Get unique participants and enrollments
        Set<UUID> participantIds = attendances.stream()
                .map(a -> a.getEnrollment().getParticipant().getId())
                .collect(Collectors.toSet());
        Set<UUID> enrollmentIds = attendances.stream()
                .map(a -> a.getEnrollment().getId())
                .collect(Collectors.toSet());

        // Build absentee trends by date
        List<AttendanceSummaryResponseDTO.AbsenteeTrendDTO> absenteeTrends = buildAbsenteeTrends(attendances);

        // Build cohort breakdown (if viewing all cohorts)
        List<AttendanceSummaryResponseDTO.CohortAttendanceDTO> cohortBreakdown = null;
        String cohortName = null;
        if (request.getCohortId() == null) {
            cohortBreakdown = buildCohortBreakdown(attendances, context.getPartnerId());
        } else {
            // Get cohort name for single cohort view
            cohortName = cohortRepository.findById(request.getCohortId())
                    .map(cohort -> cohort.getCohortName())
                    .orElse(null);
        }

        return AttendanceSummaryResponseDTO.builder()
                .cohortId(request.getCohortId())
                .cohortName(cohortName)
                .overallAttendanceRate(attendanceRate)
                .totalAttendanceRecords(totalRecords)
                .totalPresentRecords(presentRecords)
                .totalAbsentRecords(absentRecords)
                .totalParticipants((long) participantIds.size())
                .totalEnrollments((long) enrollmentIds.size())
                .absenteeTrends(absenteeTrends)
                .cohortBreakdown(cohortBreakdown)
                .build();
    }

    /**
     * Builds absentee trends grouped by date.
     */
    private List<AttendanceSummaryResponseDTO.AbsenteeTrendDTO> buildAbsenteeTrends(List<Attendance> attendances) {
        Map<LocalDate, List<Attendance>> byDate = attendances.stream()
                .collect(Collectors.groupingBy(Attendance::getSessionDate));

        return byDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    List<Attendance> dayAttendances = entry.getValue();
                    long total = dayAttendances.size();
                    long absent = dayAttendances.stream()
                            .filter(a -> a.getStatus() == AttendanceStatus.ABSENT)
                            .count();
                    BigDecimal absenteeRate = total > 0 ?
                            BigDecimal.valueOf(absent)
                                    .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100))
                                    .setScale(2, RoundingMode.HALF_UP) :
                            BigDecimal.ZERO;

                    return AttendanceSummaryResponseDTO.AbsenteeTrendDTO.builder()
                            .date(date)
                            .totalRecords(total)
                            .absentRecords(absent)
                            .absenteeRate(absenteeRate)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Builds cohort-level breakdown for all cohorts.
     */
    private List<AttendanceSummaryResponseDTO.CohortAttendanceDTO> buildCohortBreakdown(
            List<Attendance> attendances,
            String partnerId
    ) {
        // Group attendances by cohort
        Map<UUID, List<Attendance>> byCohort = attendances.stream()
                .collect(Collectors.groupingBy(a -> a.getEnrollment().getCohort().getId()));

        return byCohort.entrySet().stream()
                .map(entry -> {
                    UUID cohortId = entry.getKey();
                    List<Attendance> cohortAttendances = entry.getValue();

                    // Get cohort details from first attendance
                    Attendance firstAttendance = cohortAttendances.get(0);
                    var cohort = firstAttendance.getEnrollment().getCohort();

                    long total = cohortAttendances.size();
                    long present = cohortAttendances.stream()
                            .filter(a -> a.getStatus() == AttendanceStatus.PRESENT ||
                                       a.getStatus() == AttendanceStatus.LATE ||
                                       a.getStatus() == AttendanceStatus.EXCUSED)
                            .count();
                    long absent = cohortAttendances.stream()
                            .filter(a -> a.getStatus() == AttendanceStatus.ABSENT)
                            .count();

                    BigDecimal attendanceRate = total > 0 ?
                            BigDecimal.valueOf(present)
                                    .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100))
                                    .setScale(2, RoundingMode.HALF_UP) :
                            BigDecimal.ZERO;

                    Set<UUID> participantIds = cohortAttendances.stream()
                            .map(a -> a.getEnrollment().getParticipant().getId())
                            .collect(Collectors.toSet());

                    return AttendanceSummaryResponseDTO.CohortAttendanceDTO.builder()
                            .cohortId(cohortId)
                            .cohortName(cohort.getCohortName())
                            .cohortStartDate(cohort.getStartDate())
                            .cohortEndDate(cohort.getEndDate())
                            .cohortStatus(cohort.getStatus().name())
                            .attendanceRate(attendanceRate)
                            .totalRecords(total)
                            .presentRecords(present)
                            .absentRecords(absent)
                            .participantCount((long) participantIds.size())
                            .build();
                })
                .sorted(Comparator.comparing(AttendanceSummaryResponseDTO.CohortAttendanceDTO::getCohortStartDate).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Builds empty summary response.
     */
    private AttendanceSummaryResponseDTO buildEmptySummary(UUID cohortId) {
        String cohortName = null;
        if (cohortId != null) {
            cohortName = cohortRepository.findById(cohortId)
                    .map(cohort -> cohort.getCohortName())
                    .orElse(null);
        }

        return AttendanceSummaryResponseDTO.builder()
                .cohortId(cohortId)
                .cohortName(cohortName)
                .overallAttendanceRate(BigDecimal.ZERO)
                .totalAttendanceRecords(0L)
                .totalPresentRecords(0L)
                .totalAbsentRecords(0L)
                .totalParticipants(0L)
                .totalEnrollments(0L)
                .absenteeTrends(Collections.emptyList())
                .cohortBreakdown(Collections.emptyList())
                .build();
    }
}
