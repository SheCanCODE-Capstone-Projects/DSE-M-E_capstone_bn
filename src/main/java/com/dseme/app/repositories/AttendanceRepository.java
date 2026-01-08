package com.dseme.app.repositories;

import com.dseme.app.models.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {
    /**
     * Find attendance by enrollment, module, and session date.
     * Used to check for duplicate attendance records (idempotency).
     */
    Optional<Attendance> findByEnrollmentIdAndModuleIdAndSessionDate(
            UUID enrollmentId,
            UUID moduleId,
            LocalDate sessionDate
    );

    /**
     * Check if attendance already exists for the given enrollment, module, and date.
     * Used for idempotency checks.
     */
    boolean existsByEnrollmentIdAndModuleIdAndSessionDate(
            UUID enrollmentId,
            UUID moduleId,
            LocalDate sessionDate
    );

    /**
     * Find all attendance records for enrollments in a specific cohort within a date range.
     * Used for calculating weekly attendance rates.
     */
    @Query("SELECT a FROM Attendance a " +
           "WHERE a.enrollment.cohort.id = :cohortId " +
           "AND a.sessionDate >= :startDate " +
           "AND a.sessionDate <= :endDate")
    List<Attendance> findByCohortIdAndSessionDateBetween(
            @Param("cohortId") UUID cohortId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Count attendance records for enrollments in a specific cohort within a date range.
     * Used for calculating weekly attendance rates.
     */
    @Query("SELECT COUNT(a) FROM Attendance a " +
           "WHERE a.enrollment.cohort.id = :cohortId " +
           "AND a.sessionDate >= :startDate " +
           "AND a.sessionDate <= :endDate")
    Long countByCohortIdAndSessionDateBetween(
            @Param("cohortId") UUID cohortId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Count attendance records with PRESENT status for enrollments in a specific cohort within a date range.
     * Used for calculating attendance rates (only PRESENT counts as attended).
     */
    @Query("SELECT COUNT(a) FROM Attendance a " +
           "WHERE a.enrollment.cohort.id = :cohortId " +
           "AND a.sessionDate >= :startDate " +
           "AND a.sessionDate <= :endDate " +
           "AND a.status = 'PRESENT'")
    Long countPresentByCohortIdAndSessionDateBetween(
            @Param("cohortId") UUID cohortId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Find all attendance records for a specific enrollment.
     * Used for checking attendance gaps.
     */
    @Query("SELECT a FROM Attendance a " +
           "WHERE a.enrollment.id = :enrollmentId " +
           "AND (a.status = 'PRESENT' OR a.status = 'LATE' OR a.status = 'EXCUSED') " +
           "ORDER BY a.sessionDate DESC")
    List<Attendance> findByEnrollmentIdOrderBySessionDateDesc(@Param("enrollmentId") UUID enrollmentId);

    /**
     * Get the most recent attendance date for an enrollment.
     * Returns the latest session date where status is PRESENT, LATE, or EXCUSED.
     */
    @Query("SELECT MAX(a.sessionDate) FROM Attendance a " +
           "WHERE a.enrollment.id = :enrollmentId " +
           "AND (a.status = 'PRESENT' OR a.status = 'LATE' OR a.status = 'EXCUSED')")
    LocalDate findMostRecentAttendanceDateByEnrollmentId(@Param("enrollmentId") UUID enrollmentId);

    /**
     * Find attendance records by enrollment IDs, module ID, and date range.
     * Used for export functionality.
     */
    @Query("SELECT a FROM Attendance a " +
           "WHERE a.enrollment.id IN :enrollmentIds " +
           "AND a.module.id = :moduleId " +
           "AND a.sessionDate >= :startDate " +
           "AND a.sessionDate <= :endDate " +
           "ORDER BY a.sessionDate DESC")
    List<Attendance> findByEnrollmentIdInAndModuleIdAndSessionDateBetween(
            @Param("enrollmentIds") List<UUID> enrollmentIds,
            @Param("moduleId") UUID moduleId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Find all attendance records for a specific enrollment.
     * Used for calculating attendance percentage.
     */
    List<Attendance> findByEnrollmentId(UUID enrollmentId);
}

