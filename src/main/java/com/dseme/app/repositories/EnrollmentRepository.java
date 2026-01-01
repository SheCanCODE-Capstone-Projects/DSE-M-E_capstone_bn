package com.dseme.app.repositories;

import com.dseme.app.models.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
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
    java.util.Optional<Enrollment> findByParticipantIdAndCohortId(UUID participantId, UUID cohortId);
}

