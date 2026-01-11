package com.dseme.app.repositories;

import com.dseme.app.models.Enrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {
    /**
     * Find enrollments by participant ID.
     * Used to check if participant already has an enrollment.
     */
    List<Enrollment> findByParticipantId(UUID participantId);

    /**
     * Find enrollments by cohort ID.
     * Used for cohort-based queries.
     */
    List<Enrollment> findByCohortId(UUID cohortId);

    /**
     * Check if participant has any enrollment.
     */
    boolean existsByParticipantId(UUID participantId);

    /**
     * Check if participant is already enrolled in a specific cohort.
     * Used to prevent duplicate enrollments.
     */
    boolean existsByParticipantIdAndCohortId(UUID participantId, UUID cohortId);

    /**
     * Find enrollment by participant ID and cohort ID.
     * Used to check if enrollment already exists.
     */
    Optional<Enrollment> findByParticipantIdAndCohortId(UUID participantId, UUID cohortId);

    /**
     * Find enrollments by cohort ID and status.
     * Used to count active participants in a cohort.
     */
    List<Enrollment> findByCohortIdAndStatus(UUID cohortId, com.dseme.app.enums.EnrollmentStatus status);

    /**
     * Count enrollments by cohort ID and status.
     * Used to count active participants in a cohort.
     */
    long countByCohortIdAndStatus(UUID cohortId, com.dseme.app.enums.EnrollmentStatus status);

    /**
     * Find pending (unverified) enrollments by partner ID with pagination.
     * Used by ME_OFFICER to review enrollments pending verification.
     * Filters by participant's partner to ensure partner-level isolation.
     */
    @Query("SELECT e FROM Enrollment e WHERE e.participant.partner.partnerId = :partnerId " +
           "AND e.isVerified = false " +
           "ORDER BY e.createdAt DESC")
    Page<Enrollment> findPendingEnrollmentsByPartnerId(
            @Param("partnerId") String partnerId,
            Pageable pageable
    );

    /**
     * Find enrollment by ID and partner ID (through participant).
     * Used to ensure partner-level isolation when accessing a specific enrollment.
     */
    @Query("SELECT e FROM Enrollment e WHERE e.id = :enrollmentId " +
           "AND e.participant.partner.partnerId = :partnerId")
    Optional<Enrollment> findByIdAndParticipantPartnerPartnerId(
            @Param("enrollmentId") UUID enrollmentId,
            @Param("partnerId") String partnerId
    );

    /**
     * Find all enrollments by partner ID (through participant).
     * Used for partner-level reporting.
     */
    @Query("SELECT e FROM Enrollment e WHERE e.participant.partner.partnerId = :partnerId")
    List<Enrollment> findByParticipantPartnerPartnerId(@Param("partnerId") String partnerId);
}

