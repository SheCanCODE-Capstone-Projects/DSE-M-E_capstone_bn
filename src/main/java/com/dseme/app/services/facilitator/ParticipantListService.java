package com.dseme.app.services.facilitator;

import com.dseme.app.dtos.facilitator.*;
import com.dseme.app.enums.AttendanceStatus;
import com.dseme.app.enums.EnrollmentStatus;
import com.dseme.app.enums.Gender;
import com.dseme.app.models.Enrollment;
import com.dseme.app.models.Participant;
import com.dseme.app.repositories.AttendanceRepository;
import com.dseme.app.repositories.EnrollmentRepository;
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
 * Service for managing participant lists with pagination, search, filter, and sort.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipantListService {

    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final EnrollmentStatusService enrollmentStatusService;
    private final CohortIsolationService cohortIsolationService;

    /**
     * Gets paginated list of participants with search, filter, and sort capabilities.
     * 
     * @param context Facilitator context
     * @param request List request with pagination, search, filter, sort parameters
     * @return Paginated participant list response
     */
    public ParticipantListResponseDTO getAllParticipants(
            FacilitatorContext context,
            ParticipantListRequestDTO request
    ) {
        // Validate facilitator has active cohort
        cohortIsolationService.getFacilitatorActiveCohort(context);

        // Get all enrollments for the active cohort
        List<Enrollment> allEnrollments = enrollmentRepository.findByCohortId(context.getCohortId());

        // Check attendance gaps and update statuses if needed
        enrollmentStatusService.checkAllEnrollmentsForAttendanceGaps(context.getCohortId());

        // Refresh enrollments after status updates
        allEnrollments = enrollmentRepository.findByCohortId(context.getCohortId());

        // Convert to participant list DTOs
        List<ParticipantListDTO> participantList = allEnrollments.stream()
                .map(this::toParticipantListDTO)
                .collect(Collectors.toList());

        // Apply search filter
        if (request.getSearch() != null && !request.getSearch().trim().isEmpty()) {
            String searchTerm = request.getSearch().toLowerCase().trim();
            participantList = participantList.stream()
                    .filter(p -> 
                        (p.getFirstName() != null && p.getFirstName().toLowerCase().contains(searchTerm)) ||
                        (p.getLastName() != null && p.getLastName().toLowerCase().contains(searchTerm)) ||
                        (p.getEmail() != null && p.getEmail().toLowerCase().contains(searchTerm)) ||
                        (p.getPhone() != null && p.getPhone().contains(searchTerm))
                    )
                    .collect(Collectors.toList());
        }

        // Apply enrollment status filter
        if (request.getEnrollmentStatusFilter() != null && !request.getEnrollmentStatusFilter().trim().isEmpty()) {
            String statusFilter = request.getEnrollmentStatusFilter().toUpperCase();
            participantList = participantList.stream()
                    .filter(p -> {
                        String status = p.getEnrollmentStatus();
                        if ("INACTIVE".equals(statusFilter)) {
                            return "INACTIVE".equals(status);
                        }
                        return status.equals(statusFilter);
                    })
                    .collect(Collectors.toList());
        }

        // Apply gender filter
        if (request.getGenderFilter() != null && !request.getGenderFilter().trim().isEmpty()) {
            Gender genderFilter = Gender.valueOf(request.getGenderFilter().toUpperCase());
            participantList = participantList.stream()
                    .filter(p -> p.getGender() == genderFilter)
                    .collect(Collectors.toList());
        }

        // Apply sorting
        participantList = sortParticipantList(participantList, request.getSortBy(), request.getSortDirection());

        // Apply pagination
        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 10;
        
        int start = page * size;
        int end = Math.min(start + size, participantList.size());
        
        List<ParticipantListDTO> pagedList = start < participantList.size() 
                ? participantList.subList(start, end) 
                : new ArrayList<>();
        
        int totalPages = (int) Math.ceil((double) participantList.size() / size);

        return ParticipantListResponseDTO.builder()
                .participants(pagedList)
                .totalElements((long) participantList.size())
                .totalPages(totalPages)
                .currentPage(page)
                .pageSize(size)
                .hasNext(page < totalPages - 1)
                .hasPrevious(page > 0)
                .build();
    }

    /**
     * Converts Enrollment to ParticipantListDTO.
     */
    private ParticipantListDTO toParticipantListDTO(Enrollment enrollment) {
        Participant participant = enrollment.getParticipant();
        
        // Calculate attendance percentage
        BigDecimal attendancePercentage = calculateAttendancePercentage(enrollment);
        
        // Determine display status (INACTIVE for ENROLLED after 2-week gap)
        String displayStatus = getDisplayStatus(enrollment);

        return ParticipantListDTO.builder()
                .participantId(participant.getId())
                .firstName(participant.getFirstName())
                .lastName(participant.getLastName())
                .email(participant.getEmail())
                .phone(participant.getPhone())
                .gender(participant.getGender())
                .enrollmentDate(enrollment.getEnrollmentDate())
                .attendancePercentage(attendancePercentage)
                .enrollmentStatus(displayStatus)
                .enrollmentId(enrollment.getId())
                .build();
    }

    /**
     * Calculates attendance percentage for an enrollment.
     * Formula: (Present attendance records / Total attendance records) * 100
     */
    private BigDecimal calculateAttendancePercentage(Enrollment enrollment) {
        List<com.dseme.app.models.Attendance> allAttendances = enrollment.getAttendances();
        
        if (allAttendances.isEmpty()) {
            return BigDecimal.ZERO;
        }

        long presentCount = allAttendances.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.PRESENT || 
                            a.getStatus() == AttendanceStatus.LATE || 
                            a.getStatus() == AttendanceStatus.EXCUSED)
                .count();

        if (allAttendances.size() == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(presentCount)
                .divide(BigDecimal.valueOf(allAttendances.size()), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Gets display status for enrollment.
     * Returns "INACTIVE" for ENROLLED status after 2-week gap, otherwise returns status name.
     */
    private String getDisplayStatus(Enrollment enrollment) {
        if (enrollment.getStatus() == EnrollmentStatus.ENROLLED) {
            // Check if there's a 2-week gap (meaning they were ACTIVE before)
            LocalDate mostRecentAttendance = attendanceRepository
                    .findMostRecentAttendanceDateByEnrollmentId(enrollment.getId());
            
            if (mostRecentAttendance != null) {
                long daysSinceLastAttendance = java.time.temporal.ChronoUnit.DAYS.between(
                        mostRecentAttendance, LocalDate.now());
                if (daysSinceLastAttendance >= 14) {
                    return "INACTIVE";
                }
            }
        }
        
        return enrollment.getStatus().name();
    }

    /**
     * Sorts participant list by specified field and direction.
     */
    private List<ParticipantListDTO> sortParticipantList(
            List<ParticipantListDTO> list, 
            String sortBy, 
            String sortDirection
    ) {
        Comparator<ParticipantListDTO> comparator = null;

        switch (sortBy != null ? sortBy.toLowerCase() : "firstname") {
            case "firstname":
                comparator = Comparator.comparing(
                        p -> p.getFirstName() != null ? p.getFirstName().toLowerCase() : "",
                        Comparator.nullsLast(String::compareTo));
                break;
            case "lastname":
                comparator = Comparator.comparing(
                        p -> p.getLastName() != null ? p.getLastName().toLowerCase() : "",
                        Comparator.nullsLast(String::compareTo));
                break;
            case "email":
                comparator = Comparator.comparing(
                        p -> p.getEmail() != null ? p.getEmail().toLowerCase() : "",
                        Comparator.nullsLast(String::compareTo));
                break;
            case "phone":
                comparator = Comparator.comparing(
                        p -> p.getPhone() != null ? p.getPhone() : "",
                        Comparator.nullsLast(String::compareTo));
                break;
            case "enrollmentdate":
                comparator = Comparator.comparing(
                        ParticipantListDTO::getEnrollmentDate,
                        Comparator.nullsLast(LocalDate::compareTo));
                break;
            case "attendancepercentage":
                comparator = Comparator.comparing(
                        ParticipantListDTO::getAttendancePercentage,
                        Comparator.nullsLast(BigDecimal::compareTo));
                break;
            case "enrollmentstatus":
                comparator = Comparator.comparing(
                        p -> p.getEnrollmentStatus() != null ? p.getEnrollmentStatus() : "",
                        Comparator.nullsLast(String::compareTo));
                break;
            default:
                comparator = Comparator.comparing(
                        p -> p.getFirstName() != null ? p.getFirstName().toLowerCase() : "",
                        Comparator.nullsLast(String::compareTo));
        }

        if ("DESC".equalsIgnoreCase(sortDirection)) {
            comparator = comparator.reversed();
        }

        return list.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    /**
     * Gets participant statistics (active/inactive counts, gender distribution).
     * 
     * @param context Facilitator context
     * @return Participant statistics
     */
    public ParticipantStatisticsDTO getParticipantStatistics(FacilitatorContext context) {
        // Validate facilitator has active cohort
        cohortIsolationService.getFacilitatorActiveCohort(context);

        // Check attendance gaps and update statuses if needed
        enrollmentStatusService.checkAllEnrollmentsForAttendanceGaps(context.getCohortId());

        // Get all enrollments for the active cohort
        List<Enrollment> enrollments = enrollmentRepository.findByCohortId(context.getCohortId());

        // Count active participants (status = ACTIVE)
        long activeCount = enrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
                .count();

        // Count inactive participants (status = ENROLLED after 2-week gap)
        long inactiveCount = enrollments.stream()
                .filter(e -> {
                    if (e.getStatus() == EnrollmentStatus.ENROLLED) {
                        LocalDate mostRecentAttendance = attendanceRepository
                                .findMostRecentAttendanceDateByEnrollmentId(e.getId());
                        if (mostRecentAttendance != null) {
                            long daysSinceLastAttendance = java.time.temporal.ChronoUnit.DAYS.between(
                                    mostRecentAttendance, LocalDate.now());
                            return daysSinceLastAttendance >= 14;
                        }
                    }
                    return false;
                })
                .count();

        // Calculate gender distribution
        Map<String, Long> genderDistribution = enrollments.stream()
                .map(e -> e.getParticipant().getGender())
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        g -> g.name(),
                        Collectors.counting()
                ));

        return ParticipantStatisticsDTO.builder()
                .activeParticipantsCount(activeCount)
                .inactiveParticipantsCount(inactiveCount)
                .genderDistribution(genderDistribution)
                .totalParticipantsCount((long) enrollments.size())
                .build();
    }
}

