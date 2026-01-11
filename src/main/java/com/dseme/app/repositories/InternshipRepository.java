package com.dseme.app.repositories;

import com.dseme.app.enums.InternshipStatus;
import com.dseme.app.models.Internship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InternshipRepository extends JpaRepository<Internship, UUID> {
    /**
     * Find internships by enrollment ID.
     */
    List<Internship> findByEnrollmentId(UUID enrollmentId);

    /**
     * Find active internships by enrollment ID.
     * Used to enforce "one active internship per enrollment" rule.
     */
    List<Internship> findByEnrollmentIdAndStatus(UUID enrollmentId, InternshipStatus status);

    /**
     * Check if enrollment has an active internship.
     * Used to enforce "one active internship per enrollment" rule.
     */
    boolean existsByEnrollmentIdAndStatus(UUID enrollmentId, InternshipStatus status);

    /**
     * Find internship by ID and partner ID (through enrollment -> participant).
     * Used to ensure partner-level isolation when accessing a specific internship.
     */
    @Query("SELECT i FROM Internship i WHERE i.id = :internshipId " +
           "AND i.enrollment.participant.partner.partnerId = :partnerId")
    Optional<Internship> findByIdAndEnrollmentParticipantPartnerPartnerId(
            @Param("internshipId") UUID internshipId,
            @Param("partnerId") String partnerId
    );
}
