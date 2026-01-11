package com.dseme.app.repositories;

import com.dseme.app.models.Participant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, UUID> {
    /**
     * Find participant by email.
     * Used to check if participant already exists.
     */
    Optional<Participant> findByEmail(String email);

    /**
     * Find all participants by partner ID with pagination.
     * Used by ME_OFFICER to view all participants under their partner.
     */
    Page<Participant> findByPartnerPartnerId(String partnerId, Pageable pageable);

    /**
     * Find participants by partner ID with search (name, email, phone).
     * Used by ME_OFFICER to search participants under their partner.
     */
    @Query("SELECT p FROM Participant p WHERE p.partner.partnerId = :partnerId " +
           "AND (LOWER(p.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR p.phone LIKE CONCAT('%', :search, '%'))")
    Page<Participant> findByPartnerPartnerIdAndSearch(
            @Param("partnerId") String partnerId,
            @Param("search") String search,
            Pageable pageable
    );

    /**
     * Find participants by partner ID and verification status with pagination.
     * Used by ME_OFFICER to filter participants by verification status.
     */
    Page<Participant> findByPartnerPartnerIdAndIsVerified(String partnerId, Boolean isVerified, Pageable pageable);

    /**
     * Find participants by partner ID, verification status, and search with pagination.
     * Used by ME_OFFICER to search and filter participants by verification status.
     */
    @Query("SELECT p FROM Participant p WHERE p.partner.partnerId = :partnerId " +
           "AND p.isVerified = :isVerified " +
           "AND (LOWER(p.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR p.phone LIKE CONCAT('%', :search, '%'))")
    Page<Participant> findByPartnerPartnerIdAndIsVerifiedAndSearch(
            @Param("partnerId") String partnerId,
            @Param("isVerified") Boolean isVerified,
            @Param("search") String search,
            Pageable pageable
    );

    /**
     * Find participant by ID and partner ID.
     * Used to ensure partner-level isolation when accessing a specific participant.
     */
    Optional<Participant> findByIdAndPartnerPartnerId(UUID participantId, String partnerId);
}

