package com.dseme.app.repositories;

import com.dseme.app.models.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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
}

